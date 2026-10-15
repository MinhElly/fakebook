import { useCallback, useEffect, useRef, useState } from "react"

import api from "@/services/apis"

import { hydratePosts } from "@/utils/postHydration"

import type { Post } from "@/types"

import { useAuth } from "@/providers/AuthProvider"

export default function useProfilePosts(userId?: string) {
  const { status } = useAuth()

  const [posts, setPosts] = useState<Post[]>([])

  const [loading, setLoading] = useState(false)

  const [hasMore, setHasMore] = useState(true)

  const [error, setError] = useState<string | null>(null)

  const cursor = useRef<string | null>(null)

  const pending = useRef(false)

  const generation = useRef(0)

  const load = useCallback(
    async (reset = false) => {
      if (!userId || (pending.current && !reset)) return

      const active = generation.current

      pending.current = true
      setLoading(true)
      setError(null)

      try {
        const response = await api.get(
          `/services/postservice/api/posts/profile/${userId}`,
          { params: { cursor: reset ? undefined : cursor.current, size: 11 } },
        )

        const hydrated = await hydratePosts(response.data.items)

        if (active !== generation.current) return

        setPosts((old) =>
          reset
            ? hydrated
            : [
                ...old,
                ...hydrated.filter((p) => !old.some((x) => x.id === p.id)),
              ],
        )

        cursor.current = response.data.nextCursor
        setHasMore(response.data.hasMore)
      } catch {
        if (active === generation.current)
          setError("Không tải được bài viết trên trang cá nhân.")
      } finally {
        if (active === generation.current) {
          pending.current = false
          setLoading(false)
        }
      }
    },
    [userId, status],
  )

  useEffect(() => {
    generation.current++
    cursor.current = null
    setPosts([])
    setHasMore(true)
    void load(true)
  }, [load])

  useEffect(() => {
    const changed = (event: Event) => {
      const { post, deletedId } = (event as CustomEvent<{
        post?: Post
        deletedId?: string
      }>).detail

      if (deletedId) {
        setPosts((old) => old.filter((p) => p.id !== deletedId))
        return
      }

      if (!post) return

      const belongs =
        post.authorId === userId || post.taggedUserIds?.includes(userId || "")

      setPosts((old) => {
        if (!belongs) return old.filter((p) => p.id !== post.id)

        if (old.some((p) => p.id === post.id))
          return old.map((p) => (p.id === post.id ? post : p))

        return [...old, post].sort(
          (a, b) =>
            (b.createdAt || "").localeCompare(a.createdAt || "") ||
            b.id.localeCompare(a.id),
        )
      })
    }

    window.addEventListener("fakebook:post-changed", changed)

    return () => window.removeEventListener("fakebook:post-changed", changed)
  }, [userId])

  return {
    posts,
    loading,
    hasMore,
    error,
    loadMorePosts: () => {
      if (hasMore) void load()
    },
  }
}
