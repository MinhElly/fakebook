import { getMediaUrl } from "@/services/mediaService";
import api from "@/services/apis";
import type { Post } from "@/types";
import { fetchCommentSummaries } from "@/services/commentService";
import { fetchReactionSummaries } from "@/services/reactionService";

export async function hydratePosts(postsData: any[]): Promise<Post[]> {
  if (!postsData || postsData.length === 0) return [];

  const mappedPosts: any[] = postsData.map((dto: any) => ({
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

  // Resolve media kinds through the authorized metadata endpoint; do not expose provider delivery URLs.
  const mediaIds: string[] = [...new Set<string>(mappedPosts.flatMap(post => post.mediaIds))];
  const kinds = new Map<string, string>();
  const batches: string[][] = [];
  for (let offset = 0; offset < mediaIds.length; offset += 50) batches.push(mediaIds.slice(offset, offset + 50));
  for (let offset = 0; offset < batches.length; offset += 3) {
    await Promise.all(batches.slice(offset, offset + 3).map(async batch => {
      try {
        const { data } = await api.get<{ id: string; mediaType: string }[]>("/services/mediaservice/api/media", { params: { "id.in": batch.join(",") } });
        data.forEach(item => kinds.set(item.id, item.mediaType));
      } catch { /* The file endpoint still enforces authorization; failed metadata can be retried by refreshing the post. */ }
    }));
  }
  // File access is authorized against the current parent Post; metadata is not public.
  mappedPosts.forEach((post: any) => {
    post.image = post.mediaIds.length ? getMediaUrl(post.mediaIds[0]) : null;
    post.media = post.mediaIds.map((id: string) => ({ id, mediaType: kinds.get(id) ?? "IMAGE" }));
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
