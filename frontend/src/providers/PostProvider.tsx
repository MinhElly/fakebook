import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import { PostContext } from "@/stores/postStore";
import type { Post } from "@/types";
import api from "@/services/apis";
import { useAuth } from "@/providers/AuthProvider";
import { getFeed } from "@/services/feedService";
import useRequireLogin from "@/hooks/useRequireLogin";
import { fetchReactionSummaries } from "@/services/reactionService";
import { hydratePosts } from "@/utils/postHydration";
import { useRealtime } from "@/providers/RealtimeProvider";

export default function PostProvider({ children }: { children: React.ReactNode }) {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [pendingPost, setPendingPost] = useState<any>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const { status, user } = useAuth();
  const { subscribe } = useRealtime();
  const visiblePostIdsRef = useRef(new Set<string>());
  const cursorRef = useRef<string | null>(null);
  const pendingRef = useRef(false);
  const generationRef = useRef(0);
  const [feedMode, setFeedMode] = useState<"home" | "friends">("home");
  const [feedActive, setFeedActive] = useState(false);
  const [loginRequired, setLoginRequired] = useState(false);
  const [feedError, setFeedError] = useState<string | null>(null);
  const requireLogin = useRequireLogin();

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
    if (!feedActive || status === "initializing") return;
    if (feedMode === "friends" && status !== "authenticated") return;
    generationRef.current++;
    cursorRef.current = null;
    setPosts([]);
    setHasMore(true);
    void fetchPosts(0, true);
  }, [status, feedMode, feedActive]);

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

    const unsubscribeComment = subscribe(event => {
      if (event.eventType === "COMMENT_CHANGED" && visiblePostIdsRef.current.has(event.postId)) {
        // Tăng/giảm bộ đếm (hoặc gọi API lấy count mới). Ở đây ta gọi hàm đơn giản là tăng 1 (nếu tạo) hoặc fetch lại count
        api.get(`/services/commentservice/api/comments/count?postId.equals=${event.postId}`)
          .then(res => {
            const count = res.data as number;
            setPosts(prev => prev.map(p => p.id === event.postId ? { ...p, comments: count } : p));
          })
          .catch(() => { });
      }
    });

    return () => {
      unsubscribe();
      unsubscribeComment();
      if (refreshTimer !== undefined) window.clearTimeout(refreshTimer);
    };
  }, [status, refreshReactionPostIds, subscribe]);

  async function fetchPosts(_pageNum: number, reset = false) {
    if (pendingRef.current && !reset) return;
    const generation = generationRef.current;
    pendingRef.current = true;
    setLoading(true);
    setFeedError(null);
    try {
      const result = await getFeed(feedMode, reset ? null : cursorRef.current);
      const ids = result.items.map(item => item.postId);
      const response = ids.length ? await api.get(`/services/postservice/api/posts?id.in=${ids.join(",")}&size=50`) : { data: [] };
      const map = new Map<string, any>((response.data || []).map((p: any) => [p.id, p]));
      const hydrated = await hydratePosts(ids.map(id => map.get(id)).filter(Boolean));
      if (generation !== generationRef.current) return;
      setPosts(previous => reset ? hydrated : [...previous, ...hydrated.filter(p => !previous.some(old => old.id === p.id))]);
      cursorRef.current = result.nextCursor;
      setHasMore(result.hasMore);
      setLoginRequired(result.loginRequired);
    } catch (error) {
      if (generation === generationRef.current) setFeedError("Không tải được bảng tin. Bạn có thể thử lại.");
      console.error("Feed request failed", error);
    } finally {
      if (generation === generationRef.current) { pendingRef.current = false; setLoading(false); }
    }
  }

  function loadMorePosts() { if (hasMore && !loading) void fetchPosts(0); }
  const changeFeedMode = useCallback((mode: "home" | "friends") => {
    setFeedMode(mode);
    setFeedActive(true);
  }, []);
  const pauseFeed = useCallback(() => setFeedActive(false), []);

  async function addPost(
    content: string,
    imageFile: File | string | null,
    visibility: string = "PUBLIC",
    taggedUserIds?: string[]
  ) {
    try {
      if (!requireLogin()) return;
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

      const created = await api.post("/services/postservice/api/posts/create", {
        content: content,
        visibility: visibility,
        mediaIds: mediaIds,
        taggedUserIds: taggedUserIds || []
      });
      const [newPost] = await hydratePosts([created.data]);
      // Explicit creation opens a new snapshot; editing never resets the feed.
      generationRef.current++;
      await fetchPosts(0, true);
      window.dispatchEvent(new CustomEvent("fakebook:post-changed", { detail: { post: newPost, created: true } }));
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
    imageFile: File | string | null | undefined,
    visibility: string = "PUBLIC",
    taggedUserIds?: string[],
    version?: number
  ) {
    try {
      if (!requireLogin()) return;
      let mediaIds: string[] | undefined;

      if (imageFile instanceof File) {
        const formData = new FormData();
        formData.append("file", imageFile);
        formData.append("purpose", "POST");
        const mediaRes = await api.post("/services/mediaservice/api/media/upload", formData);
        if (!mediaRes.data?.id) throw new Error("Upload did not return a media ID");
        mediaIds = [mediaRes.data.id];
      } else if (imageFile === null) {
        mediaIds = [];
      }

      const response = await api.put(`/services/postservice/api/posts/${id}`, {
        id: id,
        content: content,
        visibility: visibility,
        mediaIds,
        taggedUserIds,
        version,
      });
      const [updated] = await hydratePosts([response.data]);
      setPosts(previous => previous.map(p => p.id === id ? updated : p));
      window.dispatchEvent(new CustomEvent("fakebook:post-changed", { detail: { post: updated } }));
    } catch (error) {
      console.error("Lỗi khi cập nhật bài viết:", error);
      setToastMessage("Có lỗi xảy ra khi cập nhật!");
      throw error;
    }
  }

  async function deletePost(id: string) {
    try {
      if (!requireLogin()) return;
      await api.delete(`/services/postservice/api/posts/${id}`);
      setPosts(prev => prev.filter(post => post.id !== id));
      window.dispatchEvent(new CustomEvent("fakebook:post-changed", { detail: { deletedId: id } }));
      setToastMessage("Đã xoá bài viết thành công!");
    } catch (error) {
      console.error("Lỗi khi xoá bài viết:", error);
      setToastMessage("Có lỗi xảy ra khi xoá bài viết!");
      throw error;
    }
  }

  return (
    <PostContext.Provider value={{
      posts, loading, hasMore, isUploading, pendingPost, toastMessage, setToastMessage, loadMorePosts, addPost, updatePost, deletePost, feedMode, changeFeedMode, pauseFeed, loginRequired, feedError, retryFeed: () => fetchPosts(0)
    }}>
      {children}
    </PostContext.Provider>
  );
}
