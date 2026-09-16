import { useState, useEffect } from "react";
import { PostContext } from "@/stores/postStore";
import type { Post } from "@/types";
import api from "@/services/apis";
import { useAuth } from "@/providers/AuthProvider";
import keycloak from "@/services/keycloak";
import { getMyFriends } from "@/services/friendsService";

export default function PostProvider({ children }: { children: React.ReactNode }) {
  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [friendIds, setFriendIds] = useState<Set<string>>(new Set());
  const { status, user } = useAuth();

  const PAGE_SIZE = 5;

  useEffect(() => {
    if (status === "authenticated") {
      fetchPosts(0, true);
    }
  }, [status]);

  async function fetchPosts(pageNum: number, isForceRefresh = false) {
    if (loading && !isForceRefresh) return;
    setLoading(true);
    try {
      let currentFriendIds = friendIds;
      if (currentFriendIds.size === 0 && user?.id) {
        try {
          const friends = await getMyFriends();
          const ids = new Set<string>();
          friends.forEach(f => {
            if (f.user?.id && f.user.id !== user.id) ids.add(f.user.id);
            if (f.friend?.id && f.friend.id !== user.id) ids.add(f.friend.id);
          });
          setFriendIds(ids);
          currentFriendIds = ids;
        } catch (e) {
          console.warn("Lỗi tải danh sách bạn bè:", e);
        }
      }

      const response = await api.get(`/services/postservice/api/posts?sort=createdAt,desc&size=${PAGE_SIZE}&page=${pageNum}`);

      let mappedPosts = response.data.map((dto: any) => ({
        id: dto.id,
        authorId: dto.authorId, // Lưu lại ID thật để Phân quyền Admin/Sửa/Xóa
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

      // Áp dụng Phân quyền (Privacy)
      const isAdmin = keycloak.hasRealmRole("ROLE_ADMIN");
      mappedPosts = mappedPosts.filter((post: any) => {
        if (isAdmin) return true; // Admin thấy tất cả
        if (post.authorId === user?.id) return true; // Tác giả thấy bài của mình
        if (post.visibility === "PUBLIC") return true; 
        if (post.visibility === "FRIENDS" && currentFriendIds.has(post.authorId)) return true;
        return false;
      });

        // 1. Fetch user profiles
        const authorIdsSet = new Set<string>(response.data.map((dto: any) => dto.authorId));
        response.data.forEach((dto: any) => {
          if (dto.taggedUserIds) {
            dto.taggedUserIds.forEach((id: string) => authorIdsSet.add(id));
          }
        });
        const authorIds = Array.from(authorIdsSet);
        if (authorIds.length > 0) {
          try {
            const idQuery = authorIds.map(id => `id.in=${id}`).join("&");
            const profileRes = await api.get(`/services/userservice/api/user-profiles/public?${idQuery}`, { timeout: 3000 });
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
          if (p.mediaIds && p.mediaIds.length > 0) allMediaIds.add(p.mediaIds[0]);
          if (p.avatarMediaId) allMediaIds.add(p.avatarMediaId);
        });

        if (allMediaIds.size > 0) {
          try {
            const mediaIdQuery = Array.from(allMediaIds).map(id => `id.in=${id}`).join("&");
            const mediaRes = await api.get(`/services/mediaservice/api/media?${mediaIdQuery}`, { timeout: 3000 });
            const mediaMap: Record<string, string> = {};
            mediaRes.data.forEach((m: any) => {
              mediaMap[m.id] = m.url;
            });

            mappedPosts.forEach((post: any) => {
              if (post.mediaIds && post.mediaIds.length > 0 && mediaMap[post.mediaIds[0]]) {
                post.image = mediaMap[post.mediaIds[0]];
              } else {
                post.image = null; // Reset nếu không tìm thấy URL thật
              }
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
              post.avatar = "/default-avatar.svg";
            });
          }
        } else {
          // Fallback cho avatar nếu không có media nào
          mappedPosts.forEach((post: any) => {
            post.avatar = "/default-avatar.svg";
          });
        }

      if (pageNum === 0) {
        setPosts(mappedPosts);
      } else {
        // BỘ LỌC CHỐNG TRÙNG LẶP (Khắc phục triệt để lỗi Duplicate Key)
        setPosts((prev) => {
          const newPosts = mappedPosts.filter((m: any) => !prev.some(p => p.id === m.id));
          return [...prev, ...newPosts];
        });
      }

      setHasMore(response.data.length === PAGE_SIZE);
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

  async function addPost(content: string, imageFile: File | string | null, visibility: string = "PUBLIC", taggedUserIds?: string[]) {
    try {
      setIsUploading(true);
      let mediaIds: string[] = [];
      if (imageFile instanceof File) {
        const formData = new FormData();
        formData.append("file", imageFile);
        const mediaRes = await api.post("/services/mediaservice/api/media/upload", formData);
        if (mediaRes.data && mediaRes.data.id) {
          mediaIds.push(mediaRes.data.id);
        }
      }

      await api.post("/services/postservice/api/posts", {
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
    }
  }

  async function updatePost(id: string, content: string, imageFile: File | string | null, visibility: string = "PUBLIC", taggedUserIds?: string[]) {
    try {
      let mediaIds: string[] = [];
      
      // Nếu imageFile là File mới -> cần upload
      if (imageFile instanceof File) {
        const formData = new FormData();
        formData.append("file", imageFile);
        const mediaRes = await api.post("/services/mediaservice/api/media/upload", formData);
        if (mediaRes.data && mediaRes.data.id) {
          mediaIds.push(mediaRes.data.id);
        }
      } 
      // Nếu imageFile là string -> giữ nguyên media cũ
      else if (typeof imageFile === "string" && imageFile.length > 0) {
        // Cần lấy mediaId cũ từ post hiện tại
        const oldPost = posts.find(p => p.id === id);
        // Tạm thời bỏ qua hoặc lấy từ api, ở đây đơn giản truyền mảng rỗng nếu ko đổi
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
      posts, loading, hasMore, isUploading, toastMessage, setToastMessage, loadMorePosts, addPost, updatePost,
      deletePost
    }}>
      {children}
    </PostContext.Provider>
  );
}
