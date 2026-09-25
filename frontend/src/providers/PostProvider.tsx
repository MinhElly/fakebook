import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import { PostContext } from "@/stores/postStore";
import type { Post } from "@/types";
import api from "@/services/apis";
import { useAuth } from "@/providers/AuthProvider";
import { getPersonalizedFeed } from "@/services/feedService";
import { fetchReactionSummaries } from "@/services/reactionService";
import { useRealtime } from "@/providers/RealtimeProvider";

export default function PostProvider({ children }: { children: React.ReactNode }) {
  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [pendingPost, setPendingPost] = useState<any>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const { status, user } = useAuth();
  const { subscribe } = useRealtime();
  const visiblePostIdsRef = useRef(new Set<string>());

  const PAGE_SIZE = 5;
  const postIdsKey = useMemo(() => posts.map(post => post.id).slice(0, 50).join(","), [posts]);

  useEffect(() => {
    visiblePostIdsRef.current = new Set(postIdsKey ? postIdsKey.split(",") : []);
  }, [postIdsKey]);

  const refreshReactionPostIds = useCallback(async (postIds: string[]) => {
    const uniquePostIds = [...new Set(postIds)].filter(Boolean).slice(0, 50);
    if (uniquePostIds.length === 0) return;

    const summaries = await fetchReactionSummaries(uniquePostIds);
    const summaryMap = new Map(summaries.map(summary => [summary.postId, summary]));
    setPosts(previous =>
      previous.map(post => ({
        ...post,
        reactionSummary: summaryMap.get(post.id) ?? post.reactionSummary,
      })),
    );
  }, []);

  useEffect(() => {
    if (status === "authenticated") {
      fetchPosts(0, true);
    }
  }, [status]);

  useEffect(() => {
    if (status !== "authenticated" || !postIdsKey) return;

    async function refreshReactions() {
      if (document.visibilityState !== "visible") return;

      try {
        await refreshReactionPostIds(postIdsKey.split(","));
      } catch (error) {
        console.warn("Không refresh được reactions", error);
      }
    }

    const intervalId = window.setInterval(() => void refreshReactions(), 60000);
    const handleFocus = () => void refreshReactions();
    window.addEventListener("focus", handleFocus);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", handleFocus);
    };
  }, [status, postIdsKey, refreshReactionPostIds]);

  useEffect(() => {
    if (status !== "authenticated") return;

    const pendingPostIds = new Set<string>();
    let refreshTimer: number | undefined;

    const flushReactionChanges = async () => {
      const changedPostIds = [...pendingPostIds];
      pendingPostIds.clear();
      if (changedPostIds.length === 0) return;

      try {
        await refreshReactionPostIds(changedPostIds);
      } catch (error) {
        console.warn("Could not refresh realtime reactions", error);
      }
    };

    const unsubscribe = subscribe(event => {
      if (
        event.eventType !== "POST_REACTION_CHANGED" ||
        !visiblePostIdsRef.current.has(event.postId)
      ) {
        return;
      }

      pendingPostIds.add(event.postId);
      if (refreshTimer !== undefined) window.clearTimeout(refreshTimer);
      refreshTimer = window.setTimeout(() => void flushReactionChanges(), 300);
    });

    return () => {
      unsubscribe();
      if (refreshTimer !== undefined) window.clearTimeout(refreshTimer);
    };
  }, [status, refreshReactionPostIds, subscribe]);

  async function fetchPosts(pageNum: number, isForceRefresh = false) {
    if (loading && !isForceRefresh) return;
    setLoading(true);
    try {
      let postsData: any[] = [];
      let fetchedCount = 0;

      // 1. Gọi feedService lấy danh sách bài viết trên timeline của user
      // Always combine personalized items with globally visible PUBLIC posts.
      // This keeps feed semantics stable when the personalized feed changes
      // between empty and non-empty.
      const [feedItems, publicRes] = await Promise.all([
        getPersonalizedFeed(pageNum, PAGE_SIZE),
        api.get(
          `/services/postservice/api/posts?visibility.equals=PUBLIC&sort=createdAt,desc&size=${PAGE_SIZE}&page=${pageNum}`
        ),
      ]);
      const personalizedPosts: any[] = [];

      if (feedItems && feedItems.length > 0) {
        const postIds = feedItems.map((item) => item.postId);
        // Hydrate bài viết chi tiết từ postService
        const postRes = await api.get(
          `/services/postservice/api/posts?id.in=${postIds.join(",")}`
        );

        // Sắp xếp bài viết theo đúng thứ tự thời gian của feedService
        const postMap = new Map<string, any>((postRes.data || []).map((p: any) => [p.id, p]));
        personalizedPosts.push(...postIds.map((id) => postMap.get(id)).filter(Boolean));
      }

      const publicPosts = publicRes.data || [];
      const postsById = new Map<string, any>();
      [...personalizedPosts, ...publicPosts].forEach((post: any) => postsById.set(post.id, post));
      postsData = [...postsById.values()]
        .sort(
          (left: any, right: any) =>
            new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
        )
        .slice(0, PAGE_SIZE);
      fetchedCount = Math.max(feedItems.length, publicPosts.length);

      const mappedPosts = postsData.map((dto: any) => ({
        id: dto.id,
        authorId: dto.authorId,
        user: dto.authorId,
        avatar: "/default-avatar.svg",
        time: new Date(dto.createdAt).toLocaleString(),
        content: dto.content,
        visibility: dto.visibility || "PUBLIC",
        taggedUserIds: dto.taggedUserIds || [],
        mediaIds: dto.mediaIds || [],
        image: null,
        likes: 0,
        comments: 0,
        shares: 0,
        liked: false,
      }));

      // 1. Fetch user profiles
      const authorIdsSet = new Set<string>(postsData.map((dto: any) => dto.authorId));
      postsData.forEach((dto: any) => {
        if (dto.taggedUserIds) {
          dto.taggedUserIds.forEach((id: string) => authorIdsSet.add(id));
        }
      });
      const authorIds = Array.from(authorIdsSet);
      if (authorIds.length > 0) {
        try {
          const profileRes = await api.get(
            `/services/userservice/api/user-profiles/public?id.in=${authorIds.join(",")}`,
            { timeout: 3000 }
          );
          const profileMap: Record<string, any> = {};

          profileRes.data.forEach((p: any) => {
            profileMap[p.id] = {
              name: p.displayName || p.username || "Người dùng",
              avatarMediaId: p.avatarMediaId
            };
          });

          mappedPosts.forEach((post: any) => {
            const author = profileMap[post.user];
            if (author) {
              post.user = author.name;
              post.avatarMediaId = author.avatarMediaId;
            } else {
              post.user = "Người dùng ẩn danh";
            }
            post.taggedUsers = (post.taggedUserIds || []).map((id: string) => ({
              id,
              name: profileMap[id]?.name || "Người dùng"
            }));
          });
        } catch (e) {
          console.warn("UserService tắt hoặc không phản hồi.");
          mappedPosts.forEach((post: any) => {
            post.user = "Tác giả (Chưa bật UserService)";
          });
        }
      }

      // 2. Fetch media URLs
      const allMediaIds = new Set<string>();
      mappedPosts.forEach((p: any) => {
        (p.mediaIds || []).forEach((id: string) => allMediaIds.add(id));
        if (p.avatarMediaId) allMediaIds.add(p.avatarMediaId);
      });

      if (allMediaIds.size > 0) {
        try {
          const mediaRes = await api.get(
            `/services/mediaservice/api/media?id.in=${Array.from(allMediaIds).join(",")}`,
            { timeout: 3000 }
          );
          const mediaMap: Record<string, string> = {};
          mediaRes.data.forEach((m: any) => {
            mediaMap[m.id] = m.url;
          });

          mappedPosts.forEach((post: any) => {
            // Faker post data can reference media records that do not exist in
            // MediaService. Keep only IDs that were actually resolved so the
            // browser does not request a guaranteed 404 for every feed load.
            post.mediaIds = (post.mediaIds || []).filter((id: string) => Boolean(mediaMap[id]));
            post.image = post.mediaIds.length > 0 ? mediaMap[post.mediaIds[0]] : null;
            if (post.avatarMediaId && mediaMap[post.avatarMediaId]) {
              post.avatar = mediaMap[post.avatarMediaId];
            } else {
              post.avatar = "/default-avatar.svg";
            }
          });
        } catch (e) {
          console.warn("MediaService tắt hoặc không phản hồi.");
          mappedPosts.forEach((post: any) => {
            post.image = null;
            post.mediaIds = [];
            post.avatar = "/default-avatar.svg";
          });
        }
      } else {
        mappedPosts.forEach((post: any) => {
          post.avatar = "/default-avatar.svg";
        });
      }

      // 3. Fetch comment counts (batch, parallel)
      try {
        const countPromises = mappedPosts.map((post: any) =>
          api.get(
            `/services/commentservice/api/comments/count?postId.equals=${post.id}`,
            { timeout: 3000 }
          )
            .then(res => ({ postId: post.id, count: res.data as number }))
            .catch(() => ({ postId: post.id, count: 0 }))
        );
        const counts = await Promise.all(countPromises);
        const countMap: Record<string, number> = {};
        counts.forEach(c => { countMap[c.postId] = c.count; });
        mappedPosts.forEach((post: any) => {
          post.comments = countMap[post.id] || 0;
        });
      } catch (e) {
        console.warn("CommentService tắt hoặc không phản hồi.");
      }

      try {
        const postIds = mappedPosts.map((post: Post) => post.id).slice(0, 50);
        const summaries = await fetchReactionSummaries(postIds);
        const summaryMap = new Map(summaries.map(summary => [summary.postId, summary]));
        mappedPosts.forEach((post: Post) => {
          post.reactionSummary = summaryMap.get(post.id);
        });
      } catch (e) {
        console.warn("Không tải được reaction summaries", e);
      }

      if (pageNum === 0) {
        setPosts(mappedPosts);
      } else {
        setPosts((prev) => {
          const newPosts = mappedPosts.filter((m: any) => !prev.some(p => p.id === m.id));
          return [...prev, ...newPosts];
        });
      }

      setHasMore(fetchedCount === PAGE_SIZE);
      setPage(pageNum);
    } catch (error) {
      console.error("Lỗi khi tải feed:", error);
    } finally {
      setLoading(false);
    }
  }

  function loadMorePosts() {
    if (hasMore && !loading) {
      fetchPosts(page + 1);
    }
  }

  async function addPost(
    content: string,
    imageFile: File | string | null,
    visibility: string = "PUBLIC",
    taggedUserIds?: string[]
  ) {
    try {
      setIsUploading(true);

      // Create pending post preview
      let imageUrl = null;
      if (imageFile instanceof File) {
        imageUrl = URL.createObjectURL(imageFile);
      }

      setPendingPost({
        id: "pending",
        user: user?.firstName || user?.username || "Bạn",
        avatar: "/default-avatar.svg",
        time: "Vừa xong",
        content: content,
        visibility: visibility,
        image: imageUrl,
        taggedUsers: []
      });

      let mediaIds: string[] = [];
      if (imageFile instanceof File) {
        const formData = new FormData();
        formData.append("file", imageFile);
        formData.append("purpose", "POST");
        const mediaRes = await api.post("/services/mediaservice/api/media/upload", formData);
        if (mediaRes.data && mediaRes.data.id) {
          mediaIds.push(mediaRes.data.id);
        }
      }

      await api.post("/services/postservice/api/posts/create", {
        content: content,
        visibility: visibility,
        mediaIds: mediaIds,
        taggedUserIds: taggedUserIds || []
      });
      // Fetch lại để có ID mới
      await fetchPosts(0, true);
      setToastMessage("Đã đăng bài viết!");
    } catch (error) {
      console.error("Lỗi khi thêm bài viết:", error);
      setToastMessage("Lỗi khi đăng bài viết!");
      throw error;
    } finally {
      setIsUploading(false);
      setPendingPost(null);
    }
  }

  async function updatePost(
    id: string,
    content: string,
    imageFile: File | string | null,
    visibility: string = "PUBLIC",
    taggedUserIds?: string[]
  ) {
    try {
      let mediaIds: string[] = [];

      if (imageFile instanceof File) {
        const formData = new FormData();
        formData.append("file", imageFile);
        formData.append("purpose", "POST");
        const mediaRes = await api.post("/services/mediaservice/api/media/upload", formData);
        if (mediaRes.data && mediaRes.data.id) {
          mediaIds.push(mediaRes.data.id);
        }
      } else if (typeof imageFile === "string" && imageFile.length > 0) {
        const oldPost = posts.find(p => p.id === id);
      }

      await api.put(`/services/postservice/api/posts/${id}`, {
        content: content,
        visibility: visibility,
        taggedUserIds: taggedUserIds || []
      });
      await fetchPosts(0, true);
    } catch (error) {
      console.error("Lỗi khi cập nhật bài viết:", error);
      setToastMessage("Có lỗi xảy ra khi cập nhật!");
      throw error;
    }
  }

  async function deletePost(id: string) {
    try {
      await api.delete(`/services/postservice/api/posts/${id}`);
      setPosts(prev => prev.filter(post => post.id !== id));
      await fetchPosts(0, true);
      setToastMessage("Đã xoá bài viết thành công!");
    } catch (error) {
      console.error("Lỗi khi xoá bài viết:", error);
      setToastMessage("Có lỗi xảy ra khi xoá bài viết!");
      throw error;
    }
  }

  return (
    <PostContext.Provider value={{
      posts, loading, hasMore, isUploading, pendingPost, toastMessage, setToastMessage, loadMorePosts, addPost, updatePost, deletePost
    }}>
      {children}
    </PostContext.Provider>
  );
}
