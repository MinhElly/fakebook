import { useState, useRef } from "react";
import { useNavigate } from "react-router";
import api from "@/services/apis";
import { usePostStore } from "@/stores/postStore";
import { useUserStore } from "@/stores/userStore";
import { useCommentStore } from "@/stores/commentStore";
import { useOutsideClick } from "@/hooks/useOutsideClick";
import CreatePostModal from "./CreatePostModal";
import CommentSection from "./CommentSection";
import PostDetailModal from "./PostDetailModal";
import { getTimeAgo } from "@/utils/timeUtils";
import MediaGrid from "./MediaGrid";
import type { Post as PostType } from "@/types";
import { useAuth } from "@/providers/AuthProvider";
import keycloak from "@/services/keycloak";
import { Emoji, EmojiStyle } from "emoji-picker-react";

interface Props {
  post: PostType;
  isModal?: boolean;
}

const REACTIONS = [
  { key: "like", unified: "1f44d", label: "Thích", color: "text-[#1877F2]" },
  { key: "love", unified: "2764-fe0f", label: "Yêu thích", color: "text-red-500" },
  { key: "haha", unified: "1f606", label: "Haha", color: "text-yellow-400" },
  { key: "wow", unified: "1f62e", label: "Wow", color: "text-yellow-400" },
  { key: "sad", unified: "1f622", label: "Buồn", color: "text-yellow-400" },
  { key: "angry", unified: "1f621", label: "Phẫn nộ", color: "text-orange-500" },
];

export default function Post({ post, isModal = false }: Props) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { profile } = useUserStore();
  const { deletePost, setToastMessage } = usePostStore();
  const { comments: commentStoreComments, fetchedPosts: commentStoreFetchedPosts } = useCommentStore();

  const [showComments, setShowComments] = useState(isModal);
  const [showMenu, setShowMenu] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showReactionPicker, setShowReactionPicker] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);

  const [reactionsData, setReactionsData] = useState<Record<string, { name: string, type: string, timestamp?: number }>>(() => {
    const saved = localStorage.getItem(`post_reactions_map_${post.id}`);
    if (saved) {
      try { return JSON.parse(saved); } catch (e) {}
    }
    // Backward compatibility for old singular likes
    const oldReaction = localStorage.getItem(`post_reaction_${post.id}`);
    if (oldReaction && user?.id) {
      return { [user.id]: { name: profile.name, type: oldReaction, timestamp: Date.now() } };
    }
    return {};
  });

  const menuRef = useRef<HTMLDivElement>(null);
  const reactionTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useOutsideClick(menuRef, () => setShowMenu(false));

  const isOwn = post.authorId === user?.id;
  const isAdmin = keycloak.hasRealmRole("ROLE_ADMIN");
  const canDelete = isOwn || isAdmin;
  
  let displayContent = post.content || "";
  let bgGradient = null;
  let location = null;

  const locMatch = displayContent.match(/\[LOC:(.+?)\](?:\n|$)/);
  if (locMatch) {
    location = locMatch[1];
    displayContent = displayContent.replace(locMatch[0], "").trim();
  }

  const bgMatch = displayContent.match(/\[BG:(.+?)\](?:\n|$)/);
  if (bgMatch) {
    bgGradient = bgMatch[1];
    displayContent = displayContent.replace(bgMatch[0], "").trim();
  }

  let commentCount = post.comments;
  if (commentStoreFetchedPosts.has(post.id)) {
    commentCount = commentStoreComments.filter(c => c.postId === post.id).length;
  } else {
    // If not fetched but we somehow have some comments (e.g. newly created locally), show the max
    const localCount = commentStoreComments.filter(c => c.postId === post.id).length;
    commentCount = Math.max(post.comments, localCount);
  }
  
  // Calculate computed values
  const myReaction = user?.id ? reactionsData[user.id]?.type : null;
  const currentReaction = REACTIONS.find(r => r.key === myReaction);
  const totalLikes = Math.max(post.likes || 0, Object.keys(reactionsData).length);
  
  const reactionStats: Record<string, { count: number, earliest: number }> = {};
  Object.values(reactionsData).forEach(r => {
    if (!reactionStats[r.type]) {
      reactionStats[r.type] = { count: 0, earliest: r.timestamp || Date.now() };
    }
    reactionStats[r.type].count++;
    if ((r.timestamp || Date.now()) < reactionStats[r.type].earliest) {
      reactionStats[r.type].earliest = r.timestamp || Date.now();
    }
  });

  const topReactions = Object.keys(reactionStats)
    .sort((a, b) => {
      const countDiff = reactionStats[b].count - reactionStats[a].count;
      if (countDiff !== 0) return countDiff;
      return reactionStats[a].earliest - reactionStats[b].earliest;
    })
    .slice(0, 3);

  const allReactors = Object.values(reactionsData)
    .sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))
    .map(r => r.name);
  const displayReactors = allReactors.slice(0, 10);
  const remainingCount = allReactors.length - 10;

  function handleAuthorClick() {
    if (post.authorId === user?.id) {
      navigate("/profile");
    } else if (post.authorId) {
      navigate(`/profile/${post.authorId}`);
    }
  }

  function handleUserClick(userId: string) {
    if (userId === user?.id) {
      navigate("/profile");
    } else {
      navigate(`/profile/${userId}`);
    }
  }

  const renderTaggedUsers = () => {
    const tagged = post.taggedUsers || [];
    if (tagged.length === 0) return null;

    if (tagged.length === 1) {
      return (
        <span className="font-normal text-[#65676B]">
          {" "}cùng với{" "}
          <span onClick={() => handleUserClick(tagged[0].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
            {tagged[0].name}
          </span>
        </span>
      );
    }
    if (tagged.length === 2) {
      return (
        <span className="font-normal text-[#65676B]">
          {" "}cùng với{" "}
          <span onClick={() => handleUserClick(tagged[0].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
            {tagged[0].name}
          </span>
          {" "}và{" "}
          <span onClick={() => handleUserClick(tagged[1].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
            {tagged[1].name}
          </span>
        </span>
      );
    }
    if (tagged.length === 3) {
      return (
        <span className="font-normal text-[#65676B]">
          {" "}cùng với{" "}
          <span onClick={() => handleUserClick(tagged[0].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
            {tagged[0].name}
          </span>
          {", "}
          <span onClick={() => handleUserClick(tagged[1].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
            {tagged[1].name}
          </span>
          {" "}và{" "}
          <span onClick={() => handleUserClick(tagged[2].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
            {tagged[2].name}
          </span>
        </span>
      );
    }
    
    // Nếu tag hơn 3 người (4 người trở lên)
    return (
      <span className="font-normal text-[#65676B]">
        {" "}cùng với{" "}
        <span onClick={() => handleUserClick(tagged[0].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
          {tagged[0].name}
        </span>
        {", "}
        <span onClick={() => handleUserClick(tagged[1].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
          {tagged[1].name}
        </span>
        {", "}
        <span onClick={() => handleUserClick(tagged[2].id)} className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
          {tagged[2].name}
        </span>
        {" "}và{" "}
        <span className="font-semibold text-[#1C1E21] hover:underline cursor-pointer">
          {tagged.length - 3} người khác
        </span>
      </span>
    );
  };

  function pickReaction(key: string) {
    if (!user?.id) return;
    
    setReactionsData(prev => {
      const next = { ...prev };
      if (myReaction === key) {
        delete next[user.id]; // Unlike
      } else {
        next[user.id] = { name: profile.name, type: key, timestamp: Date.now() }; // Change or Add reaction
      }
      localStorage.setItem(`post_reactions_map_${post.id}`, JSON.stringify(next));
      return next;
    });
    
    setShowReactionPicker(false);
  }

  const onReactionMouseLeave = () => {
    reactionTimer.current = setTimeout(() => setShowReactionPicker(false), 300);
  }
  const onReactionMouseEnter = () => {
    setShowReactionPicker(true);
    if (reactionTimer.current) clearTimeout(reactionTimer.current);
  }

  function handleLikeClick() {
    setShowReactionPicker(false);
    if (reactionTimer.current) clearTimeout(reactionTimer.current);

    if (myReaction) {
      pickReaction(myReaction); // Will delete it
    } else {
      pickReaction("like"); // Will add it
    }
  }

  // Time formatter

  // Icon helper
  const getPrivacyIcon = (visibility: string) => {
    switch (visibility) {
      case "PRIVATE": return <svg className="w-3.5 h-3.5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM9 6c0-1.66 1.34-3 3-3s3 1.34 3 3v2H9V6zm9 14H6V10h12v10zm-6-3c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z"/></svg>;
      case "FRIENDS": return <svg className="w-3.5 h-3.5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>;
      default: return <svg className="w-3.5 h-3.5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>;
    }
  };

  const renderFacebookText = (text: string) => {
    if (!text) return text;
    try {
      // @ts-ignore
      if (typeof Intl !== 'undefined' && Intl.Segmenter) {
        // @ts-ignore
        const segmenter = new Intl.Segmenter('en', { granularity: 'grapheme' });
        // @ts-ignore
        const segments = Array.from(segmenter.segment(text)).map((s: any) => s.segment);
        return segments.map((char, i) => {
          const isEmoji = /\p{Emoji_Presentation}|\p{Extended_Pictographic}/u.test(char);
          if (isEmoji) {
            const unified = Array.from(char)
              .map(c => c.codePointAt(0)?.toString(16))
              .join('-');
            return (
              <span key={i} className="inline-block mx-[1px] translate-y-[3px]">
                <Emoji unified={unified} emojiStyle={EmojiStyle.FACEBOOK} size={20} />
              </span>
            );
          }
          return char;
        });
      }
    } catch (e) {
      console.error(e);
    }
    return text;
  };

  return (
    <>
      <div className={`bg-white rounded-xl shadow-sm border border-[#E4E6EB] ${isModal ? "" : "mb-4"}`}>
        <div className="flex flex-col">
          <div className="flex items-center gap-2 px-4 pt-3">
            <img
              src={post.avatar}
              alt={post.user}
              onClick={handleAuthorClick}
              className="w-10 h-10 rounded-full object-cover cursor-pointer hover:opacity-90 transition-opacity"
            />
            <div className="flex flex-col justify-center leading-snug">
              <p className="font-semibold text-[#1C1E21] text-sm inline-block">
                <span onClick={handleAuthorClick} className="hover:underline cursor-pointer">
                  {post.user}
                </span>
                {renderTaggedUsers()}
                {location && (
                  <span className="text-[#65676B] font-normal">
                    {" "}đang ở <span className="font-semibold text-[#1C1E21] cursor-pointer hover:underline">{location}</span>
                  </span>
                )}
              </p>
              <p className="text-[#65676B] text-[13px] flex items-center gap-1.5 mt-0.5">
                <span className="hover:underline cursor-pointer relative group/time">
                  {getTimeAgo(post.time)}
                  <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 w-max bg-black/80 text-white text-[12px] font-semibold px-2.5 py-1.5 rounded-lg opacity-0 group-hover/time:opacity-100 transition-opacity pointer-events-none z-10 shadow-lg">
                    {new Date(post.time).toLocaleString('vi-VN', { weekday: 'long', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })}
                    <svg className="absolute top-full left-1/2 -translate-x-1/2 -mt-[1px] text-black/80 w-3 h-3" fill="currentColor" viewBox="0 0 24 24"><path d="M12 21l-12-18h24z"/></svg>
                  </span>
                </span>
                • {getPrivacyIcon(post.visibility || "PUBLIC")}
              </p>
            </div>

            <div ref={menuRef} className="relative ml-auto">
              <button
                onClick={() => setShowMenu(p => !p)}
                className="w-8 h-8 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors text-[#65676B]"
              >
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" />
                </svg>
              </button>

              {showMenu && (
                <div className="absolute right-0 top-full mt-1 w-64 bg-white rounded-lg shadow-xl border border-[#E4E6EB] py-2 z-10">
                  <button
                    onClick={() => { setShowMenu(false); setToastMessage("Đã lưu bài viết vào mục Đã lưu."); }}
                    className="w-full flex items-center gap-3 px-4 py-2 hover:bg-[#F0F2F5] transition-colors text-sm text-[#1C1E21] font-medium"
                  >
                    <svg className="w-5 h-5 text-[#1C1E21]" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z"/>
                    </svg>
                    Lưu bài viết
                  </button>

                  {isOwn && (
                    <>
                      <hr className="my-1 border-[#E4E6EB]" />
                      <button
                        onClick={() => { setShowEditModal(true); setShowMenu(false); }}
                        className="w-full flex items-center gap-3 px-4 py-2 hover:bg-[#F0F2F5] transition-colors text-sm text-[#1C1E21] font-medium"
                      >
                        <svg className="w-5 h-5 text-[#1C1E21]" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                        </svg>
                        Chỉnh sửa bài viết
                      </button>
                    </>
                  )}
                  {canDelete && (
                    <button
                      onClick={() => { setShowDeleteModal(true); setShowMenu(false); }}
                      className="w-full flex items-center gap-3 px-4 py-2 hover:bg-[#F0F2F5] transition-colors text-sm text-red-500 font-medium"
                    >
                      <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                      Xóa bài viết
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          <div className="px-4 mt-2 mb-1">
            {bgGradient ? (
              <div
                className="w-full min-h-[300px] flex items-center justify-center p-6 text-white text-3xl font-bold text-center rounded-lg shadow-inner"
                style={{ background: bgGradient }}
              >
                <p className="whitespace-pre-wrap">{renderFacebookText(displayContent)}</p>
              </div>
            ) : (
              <p className="text-[#1C1E21] text-[15px] leading-relaxed whitespace-pre-wrap">
                {renderFacebookText(displayContent)}
              </p>
            )}
          </div>
        </div>

        <MediaGrid mediaIds={post.mediaIds} images={post.image ? [post.image] : []} />

        <div className="px-4 pt-1">
          <div className="flex items-center justify-between text-[#65676B] text-sm pb-2 border-b border-[#E4E6EB]">
            <div className="flex items-center gap-1 relative group/reactors cursor-pointer">
              {totalLikes > 0 && (
                <>
                    <span className="flex items-center -ml-0.5">
                      {topReactions.length > 0 ? (
                        topReactions.map((type, idx) => {
                          const rObj = REACTIONS.find(r => r.key === type);
                          return rObj ? (
                            <span 
                              key={type} 
                              style={{ zIndex: 3 - idx }} 
                              className={`flex items-center justify-center rounded-full border-[2px] border-white bg-white ${idx > 0 ? '-ml-1.5' : ''}`}
                            >
                              <Emoji unified={rObj.unified} emojiStyle={EmojiStyle.FACEBOOK} size={16} />
                            </span>
                          ) : null;
                        })
                      ) : (
                        <Emoji unified="1f44d" emojiStyle={EmojiStyle.FACEBOOK} size={18} />
                      )}
                    </span>
                    <span className="hover:underline ml-1.5">{totalLikes}</span>
                    
                    {displayReactors.length > 0 && (
                      <div className="absolute bottom-full left-0 mb-1 hidden group-hover/reactors:flex flex-col z-50 bg-black/80 text-white text-[12px] px-3 py-2 rounded-lg shadow-lg whitespace-nowrap text-left leading-tight">
                        {displayReactors.map((name, i) => (
                          <span key={i} className="py-0.5 font-medium">{name}</span>
                        ))}
                        {remainingCount > 0 && (
                          <span className="py-0.5 italic text-gray-300 font-medium">và {remainingCount} người khác...</span>
                        )}
                      </div>
                    )}
                </>
              )}
            </div>
            <div className="flex gap-3">
              {(!isModal && (post.comments > 0 || commentCount > 0)) && (
                <button 
                  onClick={() => setShowDetailModal(true)} 
                  className="hover:underline"
                >
                  {commentCount > 0 ? commentCount : post.comments} bình luận
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
              onMouseLeave={onReactionMouseLeave}
            >
              {/* Reaction floating picker */}
              {showReactionPicker && (
                <div className="absolute bottom-full left-[-10px] w-max pb-1 z-20">
                  <div className="bg-white rounded-full shadow-[0_2px_12px_rgba(0,0,0,0.2)] border border-[#E4E6EB] px-2 py-1.5 flex gap-1">
                    {REACTIONS.map(r => (
                      <button
                        key={r.key}
                        onClick={(e) => { e.stopPropagation(); pickReaction(r.key); }}
                        title={r.label}
                        className="flex flex-col items-center gap-0.5 group/r hover:bg-transparent"
                      >
                        <span className="text-2xl transform group-hover/r:scale-125 group-hover/r:-translate-y-2 transition-all duration-200 origin-bottom">
                          <Emoji unified={r.unified} emojiStyle={EmojiStyle.FACEBOOK} size={32} />
                        </span>
                        <span className="absolute -top-7 text-[10px] font-bold text-white bg-black/75 px-1.5 py-0.5 rounded-full opacity-0 group-hover/r:opacity-100 transition-opacity whitespace-nowrap">
                          {r.label}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
              {/* Nút Like */}
              <button
                onClick={handleLikeClick}
                className={`w-full flex items-center justify-center gap-2 py-1.5 hover:bg-[#F0F2F5] rounded-md transition-colors ${
                  currentReaction ? currentReaction.color : "text-[#65676B]"
                } font-semibold text-sm`}
              >
                {currentReaction ? (
                  <Emoji unified={currentReaction.unified} emojiStyle={EmojiStyle.FACEBOOK} size={20} />
                ) : (
                  <svg className="w-5 h-5 text-[#65676B]" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
                  </svg>
                )}
                {currentReaction ? currentReaction.label : "Thích"}
              </button>
            </div>

            <button
              onClick={() => isModal ? setShowComments(p => !p) : setShowDetailModal(true)}
              className="flex-1 flex items-center justify-center gap-2 py-1.5 hover:bg-[#F0F2F5] rounded-md transition-colors text-sm font-semibold text-[#65676B]"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" strokeLinejoin="round" strokeLinecap="round"/></svg>
              Bình luận
            </button>
            <button className="flex-1 flex items-center justify-center gap-2 py-1.5 hover:bg-[#F0F2F5] rounded-md transition-colors text-sm font-semibold text-[#65676B]">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" strokeLinejoin="round" strokeLinecap="round"/></svg>
              Chia sẻ
            </button>
          </div>
        </div>

        <CommentSection postId={post.id} initialVisible={showComments} isModal={isModal} />
      </div>

      {!isModal && showDetailModal && (
        <PostDetailModal post={post} onClose={() => setShowDetailModal(false)} />
      )}

      {showEditModal && (
        <CreatePostModal onClose={() => setShowEditModal(false)} editPost={post} />
      )}

      {showDeleteModal && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-white/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-[500px] rounded-xl bg-white shadow-[0_12px_28px_rgba(0,0,0,0.2)] border border-[#E4E6EB]">
            <div className="flex items-center justify-between border-b border-[#E4E6EB] px-4 py-3">
              <h3 className="text-xl font-bold text-[#1C1E21]">Xóa bài viết?</h3>
              <button
                onClick={() => setShowDeleteModal(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-[#E4E6EB] text-[#65676B] hover:bg-[#D8DADF]"
              >
                <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="p-4 text-[#65676B] text-[15px]">
              Bạn có chắc chắn muốn xóa bài viết này không? Bài viết sẽ bị gỡ vĩnh viễn khỏi trang cá nhân của bạn.
            </div>
            <div className="flex items-center justify-end gap-2 border-t border-[#E4E6EB] p-4">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="rounded-md px-5 py-2 font-semibold text-[#1877F2] hover:bg-[#F0F2F5]"
              >
                Hủy
              </button>
              <button
                onClick={() => {
                  deletePost(post.id);
                  setShowDeleteModal(false);
                }}
                className="rounded-md bg-[#1877F2] px-10 py-2 font-semibold text-white hover:bg-[#166FE5]"
              >
                Xóa
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
