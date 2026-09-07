import { useState, useRef } from "react";
import { useNavigate } from "react-router";
import { usePostStore } from "@/stores/postStore";
import { useUserStore } from "@/stores/userStore";
import { useCommentStore } from "@/stores/commentStore";
import { useOutsideClick } from "@/hooks/useOutsideClick";
import CreatePostModal from "./CreatePostModal";
import CommentSection from "./CommentSection";
import type { Post as PostType } from "@/types";

interface Props {
  post: PostType;
}

const REACTIONS = [
  { key: "like",  emoji: "👍", label: "Thích",      color: "text-[#1877F2]" },
  { key: "love",  emoji: "❤️", label: "Yêu thích",  color: "text-red-500" },
  { key: "haha",  emoji: "😂", label: "Haha",        color: "text-yellow-400" },
  { key: "wow",   emoji: "😮", label: "Wow",         color: "text-yellow-400" },
  { key: "sad",   emoji: "😢", label: "Buồn",        color: "text-yellow-400" },
  { key: "angry", emoji: "😡", label: "Phẫn nộ",    color: "text-orange-500" },
];

export default function Post({ post }: Props) {
  const navigate = useNavigate();
  const { deletePost } = usePostStore();
  const { profile } = useUserStore();
  const { getPostComments } = useCommentStore();

  const [reaction, setReaction] = useState<string | null>(post.liked ? "like" : null);
  const [likes, setLikes] = useState(post.likes);
  const [showComments, setShowComments] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showReactionPicker, setShowReactionPicker] = useState(false);

  const menuRef = useRef<HTMLDivElement>(null);
  const reactionTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useOutsideClick(menuRef, () => setShowMenu(false));

  const isOwn = post.user === profile.name;
  const commentCount = getPostComments(post.id).length;
  const currentReaction = REACTIONS.find(r => r.key === reaction);

  function handleAuthorClick() {
    if (post.user === profile.name) {
      navigate("/profile");
    }
  }

  function pickReaction(key: string) {
    if (reaction === key) {
      setReaction(null);
      setLikes(p => p - 1);
    } else {
      if (!reaction) setLikes(p => p + 1);
      setReaction(key);
    }
    setShowReactionPicker(false);
  }

  function handleLikeClick() {
    if (reaction) {
      setReaction(null);
      setLikes(p => p - 1);
    } else {
      setReaction("like");
      setLikes(p => p + 1);
    }
  }

  function onReactionMouseEnter() {
    reactionTimer.current = setTimeout(() => setShowReactionPicker(true), 400);
  }
  function onReactionMouseLeave() {
    if (reactionTimer.current) clearTimeout(reactionTimer.current);
  }

  return (
    <>
      <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] mb-3">
        {/* Header */}
        <div className="p-4 pb-2">
          <div className="flex items-center gap-3">
            <img
              src={post.avatar}
              alt={post.user}
              onClick={handleAuthorClick}
              className="w-10 h-10 rounded-full object-cover cursor-pointer hover:opacity-90 transition-opacity"
            />
            <div>
              <p
                onClick={handleAuthorClick}
                className="font-semibold text-[#1C1E21] text-sm hover:underline cursor-pointer"
              >
                {post.user}
              </p>
              <p className="text-[#65676B] text-xs">{post.time} · 🌍</p>
            </div>

            <div ref={menuRef} className="relative ml-auto">
              <button
                onClick={() => setShowMenu(p => !p)}
                className="text-[#65676B] hover:bg-[#F0F2F5] rounded-full p-2 transition-colors"
              >
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20"><path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z"/></svg>
              </button>
              {showMenu && (
                <div className="absolute right-0 top-full mt-1 bg-white rounded-xl shadow-xl border border-[#E4E6EB] w-56 z-10 overflow-hidden py-1">
                  {isOwn && (
                    <>
                      <button
                        onClick={() => { setShowEditModal(true); setShowMenu(false); }}
                        className="flex items-center gap-3 w-full px-3 py-2.5 hover:bg-[#F0F2F5] text-sm font-medium text-[#1C1E21] transition-colors"
                      >
                        <span className="w-8 h-8 bg-[#E4E6EB] rounded-full flex items-center justify-center flex-shrink-0">
                          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/></svg>
                        </span>
                        Chỉnh sửa bài viết
                      </button>
                      <button
                        onClick={() => { deletePost(post.id); setShowMenu(false); }}
                        className="flex items-center gap-3 w-full px-3 py-2.5 hover:bg-[#F0F2F5] text-sm font-medium text-red-600 transition-colors"
                      >
                        <span className="w-8 h-8 bg-[#E4E6EB] rounded-full flex items-center justify-center flex-shrink-0">
                          <svg className="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd"/></svg>
                        </span>
                        Xóa bài viết
                      </button>
                      <hr className="border-[#E4E6EB] my-1" />
                    </>
                  )}
                  <button className="flex items-center gap-3 w-full px-3 py-2.5 hover:bg-[#F0F2F5] text-sm font-medium text-[#1C1E21] transition-colors">
                    <span className="w-8 h-8 bg-[#E4E6EB] rounded-full flex items-center justify-center flex-shrink-0">🔖</span>
                    Lưu bài viết
                  </button>
                  <button className="flex items-center gap-3 w-full px-3 py-2.5 hover:bg-[#F0F2F5] text-sm font-medium text-[#1C1E21] transition-colors">
                    <span className="w-8 h-8 bg-[#E4E6EB] rounded-full flex items-center justify-center flex-shrink-0">🚫</span>
                    Ẩn bài viết
                  </button>
                </div>
              )}
            </div>
          </div>

          <p className="text-[#1C1E21] text-sm leading-relaxed mt-3">{post.content}</p>
        </div>

        {post.image && (
          <img src={post.image} alt="post" className="w-full object-cover max-h-96" />
        )}

        {/* Reaction summary */}
        <div className="px-4 pt-2">
          <div className="flex items-center justify-between text-[#65676B] text-sm pb-2 border-b border-[#E4E6EB]">
            <div className="flex items-center gap-1">
              {likes > 0 && (
                <>
                  <span className="flex items-center">
                    <span className="bg-[#1877F2] rounded-full w-5 h-5 flex items-center justify-center text-[11px] border border-white">👍</span>
                    <span className="bg-red-500 rounded-full w-5 h-5 flex items-center justify-center text-[11px] border border-white -ml-1">❤️</span>
                  </span>
                  <span className="hover:underline cursor-pointer ml-1">{likes}</span>
                </>
              )}
            </div>
            <div className="flex gap-3">
              {commentCount > 0 && (
                <button onClick={() => setShowComments(p => !p)} className="hover:underline">
                  {commentCount} bình luận
                </button>
              )}
              {post.shares > 0 && (
                <span className="hover:underline cursor-pointer">{post.shares} lượt chia sẻ</span>
              )}
            </div>
          </div>
        </div>

        {/* Action buttons */}
        <div className="px-4">
          <div className="flex items-center py-1 border-b border-[#E4E6EB]">

            {/* Like button with reaction picker */}
            <div
              className="flex-1 relative"
              onMouseEnter={onReactionMouseEnter}
              onMouseLeave={() => { onReactionMouseLeave(); setShowReactionPicker(false); }}
            >
              {/* Reaction floating picker */}
              {showReactionPicker && (
                <div className="absolute bottom-full left-0 mb-1 bg-white rounded-full shadow-xl border border-[#E4E6EB] px-2 py-1.5 flex gap-1 z-20">
                  {REACTIONS.map(r => (
                    <button
                      key={r.key}
                      onClick={() => pickReaction(r.key)}
                      title={r.label}
                      className="flex flex-col items-center gap-0.5 group/r"
                    >
                      <span className="text-2xl transform group-hover/r:scale-125 transition-transform duration-150 origin-bottom">
                        {r.emoji}
                      </span>
                      <span className="text-[9px] font-semibold text-[#65676B] opacity-0 group-hover/r:opacity-100 transition-opacity whitespace-nowrap">
                        {r.label}
                      </span>
                    </button>
                  ))}
                </div>
              )}
              <button
                onClick={handleLikeClick}
                className={`w-full flex items-center justify-center gap-2 py-2 rounded-lg hover:bg-[#F0F2F5] transition-colors text-sm font-semibold ${currentReaction ? currentReaction.color : "text-[#65676B]"}`}
              >
                {currentReaction ? (
                  <>
                    <span className="text-base">{currentReaction.emoji}</span>
                    {currentReaction.label}
                  </>
                ) : (
                  <>
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                      <path d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" strokeLinejoin="round" strokeLinecap="round"/>
                    </svg>
                    Thích
                  </>
                )}
              </button>
            </div>

            <button
              onClick={() => setShowComments(p => !p)}
              className="flex-1 flex items-center justify-center gap-2 py-2 rounded-lg hover:bg-[#F0F2F5] transition-colors text-sm font-semibold text-[#65676B]"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" strokeLinejoin="round" strokeLinecap="round"/></svg>
              Bình luận
            </button>
            <button className="flex-1 flex items-center justify-center gap-2 py-2 rounded-lg hover:bg-[#F0F2F5] transition-colors text-sm font-semibold text-[#65676B]">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" strokeLinejoin="round" strokeLinecap="round"/></svg>
              Chia sẻ
            </button>
          </div>
        </div>

        <CommentSection postId={post.id} initialVisible={showComments} />
      </div>

      {showEditModal && (
        <CreatePostModal onClose={() => setShowEditModal(false)} editPost={post} />
      )}
    </>
  );
}
