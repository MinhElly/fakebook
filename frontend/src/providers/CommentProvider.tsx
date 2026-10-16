import { useState, useCallback, useLayoutEffect } from "react"

import { CommentContext } from "@/stores/commentStore"

import type { Comment } from "@/types"

import {
  getCommentsByPostId,
  createComment as apiCreateComment,
  updateComment as apiUpdateComment,
  deleteComment as apiDeleteComment,
} from "@/services/commentService"

import { useAuth } from "@/providers/AuthProvider"

import { useUserStore } from "@/stores/userStore"

import { getTimeAgo } from "@/utils/timeUtils"

import { useRealtime } from "@/providers/RealtimeProvider"

import { useEffect, useRef } from "react"

import useRequireLogin from "@/hooks/useRequireLogin"

export default function CommentProvider({
  children,
}: {
  children: React.ReactNode
}) {
  const requireLogin = useRequireLogin()

  const [comments, setComments] = useState<Comment[]>([])

  const [fetchedPosts, setFetchedPosts] = useState<Set<string>>(new Set())

  const { user, status } = useAuth()

  const [loadingPosts, setLoadingPosts] = useState<Set<string>>(new Set())

  const [errors, setErrors] = useState<Record<string, string>>({})

  const fetchedRef = useRef(new Set<string>())

  const inFlight = useRef(new Map<string, Promise<void>>())

  const generation = useRef(0)

  const refreshNeeded = useRef(new Set<string>())

  const revisions = useRef(new Map<string, number>())

  const mutations = useRef(new Set<string>())

  useLayoutEffect(() => {
    generation.current++

    fetchedRef.current.clear()
    inFlight.current.clear()
    mutations.current.clear()
    refreshNeeded.current.clear()
    revisions.current.clear()

    setComments([])
    setFetchedPosts(new Set())
    setLoadingPosts(new Set())
    setErrors({})

    return () => {
      generation.current++
    }
  }, [status, user?.id])

  const { profile } = useUserStore()

  const { subscribe } = useRealtime()

  const fetchComments = useCallback(
    async (postId: string, force = false) => {
      if (!force && fetchedRef.current.has(postId)) return

      const existing = inFlight.current.get(postId)

      if (existing) {
        if (force) refreshNeeded.current.add(postId)

        return existing
      }

      const request = generation.current

      const revision = revisions.current.get(postId) || 0

      setLoadingPosts((prev) => new Set(prev).add(postId))

      setErrors((prev) => {
        const next = { ...prev }
        delete next[postId]
        return next
      })

      const task = (async () => {
        try {
          const dtos = await getCommentsByPostId(postId)

          // Fetch user profiles for comments

          const authorIdsSet = new Set<string>()

          dtos.forEach((dto) => authorIdsSet.add(dto.authorId))

          const authorIds = Array.from(authorIdsSet)

          const profileMap: Record<string, any> = {}

          if (authorIds.length > 0) {
            try {
              const { default: api } = await import("@/services/apis")

              const idQuery = authorIds.map((id) => `id.in=${id}`).join("&")

              const profileRes = await api.get(
                `/services/userservice/api/user-profiles/public?${idQuery}`,
                { timeout: 3000 },
              )

              profileRes.data.forEach((p: any) => {
                profileMap[p.id] = {
                  name: p.displayName || p.username || "Người dùng",

                  avatar: p.avatarMediaId
                    ? `/services/mediaservice/api/media/${p.avatarMediaId}/file`
                    : "/default-avatar.svg",
                }
              })
            } catch (e) {
              console.warn("UserService tắt hoặc không phản hồi.")
            }
          }

          const newComments: Comment[] = dtos.map((dto) => ({
            id: dto.id,

            postId: dto.postId,

            parentId: dto.parentComment?.id || null,

            authorId: dto.authorId,

            user: profileMap[dto.authorId]?.name || "Người dùng",

            avatar: profileMap[dto.authorId]?.avatar || "/default-avatar.svg",

            content: dto.content,

            editedAt: dto.editedAt,

            editCount: dto.editCount,

            time: dto.createdAt ? getTimeAgo(dto.createdAt) : "Vừa xong",

            timestamp: dto.createdAt
              ? new Date(dto.createdAt).getTime()
              : Date.now(),

            likes: dto.likeCount || 0,

            liked: dto.likedByCurrentUser || false,
          }))

          if (request !== generation.current) return

          if (revision !== (revisions.current.get(postId) || 0)) {
            refreshNeeded.current.add(postId)
            return
          }

          setComments((prev) => {
            const filtered = prev.filter((c) => c.postId !== postId)

            return [...filtered, ...newComments]
          })

          fetchedRef.current.add(postId)

          setFetchedPosts((prev) => new Set(prev).add(postId))
        } catch (e) {
          if (request === generation.current)
            setErrors((prev) => ({
              ...prev,
              [postId]: "Không tải được bình luận.",
            }))
        } finally {
          if (request === generation.current) {
            inFlight.current.delete(postId)

            setLoadingPosts((prev) => {
              const next = new Set(prev)
              next.delete(postId)
              return next
            })

            if (refreshNeeded.current.delete(postId))
              void fetchComments(postId, true)
          }
        }
      })()

      inFlight.current.set(postId, task)

      return task
    },
    [],
  )

  // Dùng Ref để gọi hàm mới nhất mà không phải re-subscribe

  const fetchCommentsRef = useRef(fetchComments)

  useEffect(() => {
    fetchCommentsRef.current = fetchComments
  }, [fetchComments])

  useEffect(() => {
    if (status !== "authenticated") return

    const pendingPostIds = new Set<string>()

    let refreshTimer: number | undefined

    const flushCommentChanges = () => {
      const changedPostIds = [...pendingPostIds]

      pendingPostIds.clear()

      if (changedPostIds.length === 0) return

      changedPostIds.forEach((postId) => {
        if (fetchedRef.current.has(postId) || inFlight.current.has(postId))
          void fetchCommentsRef.current(postId, true)
      })
    }

    const unsubscribe = subscribe((event) => {
      if (event.eventType !== "COMMENT_CHANGED") {
        return
      }

      pendingPostIds.add(event.postId)

      if (refreshTimer !== undefined) window.clearTimeout(refreshTimer)

      refreshTimer = window.setTimeout(() => void flushCommentChanges(), 300)
    })

    return () => {
      unsubscribe()

      if (refreshTimer !== undefined) window.clearTimeout(refreshTimer)
    }
  }, [status, subscribe])

  async function createComment(
    postId: string,
    content: string,
    parentId: string | null = null,
  ) {
    if (!requireLogin()) return null

    const request = generation.current

    const dto = await apiCreateComment(postId, content, parentId)

    if (request !== generation.current) return null

    if (dto) {
      revisions.current.set(postId, (revisions.current.get(postId) || 0) + 1)

      const newComment: Comment = {
        id: dto.id,

        postId: dto.postId,

        parentId: dto.parentComment?.id || null,

        authorId: dto.authorId || user?.id || "",

        user: profile?.name || user?.firstName || user?.username || "Bạn",

        avatar: profile?.avatar || "/default-avatar.svg",

        content: dto.content,

        editedAt: dto.editedAt,

        editCount: dto.editCount,

        time: dto.createdAt ? getTimeAgo(dto.createdAt) : "Vừa xong",

        timestamp: dto.createdAt
          ? new Date(dto.createdAt).getTime()
          : Date.now(),

        likes: 0,

        liked: false,
      }

      setComments((prev) => [
        ...prev.filter((c) => c.id !== dto.id),
        newComment,
      ])

      return newComment.id
    }

    return null
  }

  async function updateComment(id: string, content: string): Promise<boolean> {
    if (!requireLogin() || mutations.current.has(id)) return false

    mutations.current.add(id)

    const request = generation.current

    try {
      const dto = await apiUpdateComment(id, content)

      if (request !== generation.current || !dto) return false

      revisions.current.set(
        dto.postId,
        (revisions.current.get(dto.postId) || 0) + 1,
      )

      setComments((prev) =>
        prev.map((c) =>
          c.id === id
            ? {
                ...c,
                content: dto.content,
                editedAt: dto.editedAt,
                editCount: dto.editCount,
              }
            : c,
        ),
      )

      return true
    } finally {
      mutations.current.delete(id)
    }
  }

  async function deleteComment(id: string): Promise<boolean> {
    if (!requireLogin() || mutations.current.has(id)) return false

    mutations.current.add(id)

    const request = generation.current

    try {
      const success = await apiDeleteComment(id)

      if (request !== generation.current || !success) return false

      const postId = comments.find((c) => c.id === id)?.postId

      if (postId)
        revisions.current.set(postId, (revisions.current.get(postId) || 0) + 1)

      setComments((prev) =>
        prev.filter((c) => c.id !== id && c.parentId !== id),
      )

      return true
    } finally {
      mutations.current.delete(id)
    }
  }

  async function toggleLike(id: string) {
    if (!requireLogin()) return

    // Existing comment reaction UI remains outside this feature scope.

    setComments((prev) =>
      prev.map((c) => {
        if (c.id === id) {
          return {
            ...c,
            liked: !c.liked,
            likes: c.liked ? Math.max(0, c.likes - 1) : c.likes + 1,
          }
        }

        return c
      }),
    )
  }

  function getPostComments(postId: string) {
    return comments.filter((c) => c.postId === postId && !c.parentId)
  }

  function getReplies(parentId: string) {
    return comments
      .filter((c) => c.parentId === parentId)
      .sort((a, b) => a.timestamp - b.timestamp)
  }

  return (
    <CommentContext.Provider
      value={{
        comments,
        createComment,
        updateComment,
        deleteComment,
        toggleLike,
        getPostComments,
        getReplies,
        fetchComments,
        fetchedPosts,
        loadingPosts,
        errors,
      }}
    >
      {children}
    </CommentContext.Provider>
  )
}
