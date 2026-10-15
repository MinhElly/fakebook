import { getMediaUrl } from "@/services/mediaService";
import api from "@/services/apis";
import type { Post } from "@/types";
import { fetchCommentSummaries } from "@/services/commentService";
import { fetchReactionSummaries } from "@/services/reactionService";

export async function hydratePosts(postsData: any[]): Promise<Post[]> {
  if (!postsData || postsData.length === 0) return [];

  const mappedPosts = postsData.map((dto: any) => ({
    id: dto.id,
    version: dto.version,
    createdAt: dto.createdAt,
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

  // File access is authorized against the current parent Post; metadata is not public.
  mappedPosts.forEach((post: any) => {
    post.image = post.mediaIds.length ? getMediaUrl(post.mediaIds[0]) : null;
    post.avatar = post.avatarMediaId ? getMediaUrl(post.avatarMediaId) : "/default-avatar.svg";
  });

  // 3. Fetch comment summaries
  try {
    const postIds = mappedPosts.map((post: any) => post.id).slice(0, 50);
    const summaries = await fetchCommentSummaries(postIds);
    const summaryMap = new Map(summaries.map(summary => [summary.postId, summary]));
    mappedPosts.forEach((post: any) => {
      const summary = summaryMap.get(post.id);
      post.comments = summary?.commentCount ?? 0;
      post.previewComment = summary?.previewComment ?? null;
    });
  } catch (e) {
    console.warn("CommentService tắt hoặc không phản hồi.");
    mappedPosts.forEach((post: any) => {
      post.comments = 0;
      post.previewComment = null;
    });
  }

  // 4. Fetch reaction summaries
  try {
    const postIds = mappedPosts.map((post: any) => post.id).slice(0, 50);
    const summaries = await fetchReactionSummaries(postIds);
    const summaryMap = new Map(summaries.map(summary => [summary.postId, summary]));
    mappedPosts.forEach((post: any) => {
      post.reactionSummary = summaryMap.get(post.id);
    });
  } catch (e) {
    console.warn("Không tải được reaction summaries", e);
  }

  return mappedPosts as Post[];
}
