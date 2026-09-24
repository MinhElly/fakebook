import { useState, useRef, useEffect } from "react";
import { Link } from "react-router";
import { useCommentStore } from "@/stores/commentStore";
import { useUserStore } from "@/stores/userStore";
import { useOutsideClick } from "@/hooks/useOutsideClick";
import EmojiPicker, { Emoji, EmojiStyle, Theme } from "emoji-picker-react";
import type { Comment } from "@/types";

const REACTIONS = [
  { key: "like",  unified: "1f44d",     label: "Thích",     color: "text-[#1877F2]" },
  { key: "love",  unified: "2764-fe0f", label: "Yêu thích", color: "text-red-500" },
  { key: "haha",  unified: "1f606",     label: "Haha",      color: "text-yellow-400" },
  { key: "wow",   unified: "1f62e",     label: "Wow",       color: "text-yellow-400" },
  { key: "sad",   unified: "1f622",     label: "Buồn",      color: "text-yellow-400" },
  { key: "angry", unified: "1f621",     label: "Phẫn nộ",   color: "text-orange-500" },
];

// ─── Single comment item ───────────────────────────────────────────────────────

interface CommentItemProps {
  comment: Comment;
  postId: string;
  depth?: number;
}

function CommentItem({ comment, postId, depth = 0 }: CommentItemProps) {
  const { updateComment, deleteComment, toggleLike, getReplies, createComment } = useCommentStore();
  const { profile } = useUserStore();

  const [editing, setEditing] = useState(false);
  const [editText, setEditText] = useState(comment.content);
  const [showReplyBox, setShowReplyBox] = useState(false);
  const [replyText, setReplyText] = useState("");
  const [showReplies, setShowReplies] = useState(depth === 0);
  const [showMenu, setShowMenu] = useState(false);
  const [commentReaction, setCommentReaction] = useState<string | null>(comment.liked ? "like" : null);
  const [showReactionPicker, setShowReactionPicker] = useState(false);

  const menuRef = useRef<HTMLDivElement>(null);
  const editRef = useRef<HTMLTextAreaElement>(null);
  const replyRef = useRef<HTMLInputElement>(null);
  const replyEmojiRef = useRef<HTMLDivElement>(null);
  const reactionTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [showReplyEmoji, setShowReplyEmoji] = useState(false);
  useOutsideClick(replyEmojiRef, () => setShowReplyEmoji(false));

  const currentReaction = REACTIONS.find(r => r.key === commentReaction);

  function pickCommentReaction(key: string) {
    const wasReacted = !!commentReaction;
    if (commentReaction === key) {
      setCommentReaction(null);
      if (wasReacted) toggleLike(comment.id);
    } else {
      if (!wasReacted) toggleLike(comment.id);
      setCommentReaction(key);
    }
    setShowReactionPicker(false);
  }

  function handleCommentLikeClick() {
    if (commentReaction) {
      setCommentReaction(null);
      toggleLike(comment.id);
    } else {
      setCommentReaction("like");
      toggleLike(comment.id);
    }
  }

  useOutsideClick(menuRef, () => setShowMenu(false));

  const replies = getReplies(comment.id);
  const isOwn = comment.user === profile.name;

  useEffect(() => {
    if (editing) editRef.current?.focus();
  }, [editing]);

  useEffect(() => {
    if (showReplyBox) replyRef.current?.focus();
  }, [showReplyBox]);

  function handleSaveEdit() {
    if (!editText.trim()) return;
    updateComment(comment.id, editText);
    setEditing(false);
  }

  function handleReply() {
    if (!replyText.trim()) return;
    createComment(postId, replyText, comment.id);
    setReplyText("");
    setShowReplyBox(false);
    setShowReplies(true);
  }

  return (
    <div className={`relative flex gap-2 ${depth > 0 ? "mt-2" : "mt-3"}`}>
      {/* Tree vertical line from parent to children */}
      {depth === 0 && (showReplies || showReplyBox) && replies.length > 0 && (
        <div className="absolute left-4 top-10 bottom-2 w-0.5 bg-[#E4E6EB] -ml-px z-0 rounded-full"></div>
      )}

      {/* Tree curved line for child */}
      {depth > 0 && (
        <div className="absolute -left-6 -top-2 w-6 h-5 border-l-2 border-b-2 border-[#E4E6EB] rounded-bl-xl z-0 pointer-events-none"></div>
      )}

      <div className="relative z-10 flex-shrink-0">
        <img src={comment.avatar} alt={comment.user} className={`rounded-full object-cover bg-white ${depth > 0 ? "w-6 h-6" : "w-8 h-8"}`} />
      </div>

      <div className="flex-1 min-w-0">
        {/* Bubble */}
        {editing ? (
          <div className="bg-[#F0F2F5] rounded-2xl px-3 py-2">
            <p className="font-semibold text-[#1C1E21] text-xs mb-1">{comment.user}</p>
            <textarea
              ref={editRef}
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              className="w-full bg-transparent outline-none text-sm text-[#1C1E21] resize-none leading-snug"
              rows={2}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSaveEdit(); }
                if (e.key === "Escape") { setEditing(false); setEditText(comment.content); }
              }}
            />
            <div className="flex gap-2 mt-1">
              <button onClick={handleSaveEdit} className="text-xs text-[#1877F2] font-semibold hover:underline">Lưu</button>
              <span className="text-xs text-[#65676B]">·</span>
              <button onClick={() => { setEditing(false); setEditText(comment.content); }} className="text-xs text-[#65676B] hover:underline">Hủy</button>
            </div>
          </div>
        ) : (
          <div className="group flex items-start gap-1">
            <div className="relative">
              <div className="bg-[#F0F2F5] rounded-2xl px-3 py-2 inline-block max-w-full">
                <p className="font-semibold text-[#1C1E21] text-xs leading-none mb-0.5">
                  <Link to={`/profile/${comment.authorId || comment.user}`} className="hover:underline">{comment.user}</Link>
                </p>
                <p className="text-sm text-[#1C1E21] leading-snug whitespace-pre-wrap break-words">{comment.content}</p>
              </div>
              
              {comment.likes > 0 && (
                <div className="absolute -bottom-2 -right-3 flex items-center gap-1 bg-white rounded-full shadow-sm border border-[#E4E6EB] px-1.5 py-0.5 text-xs text-[#65676B] cursor-pointer hover:underline z-10">
                  <span className="flex items-center -ml-0.5">
                    <Emoji unified={currentReaction ? currentReaction.unified : "1f44d"} emojiStyle={EmojiStyle.FACEBOOK} size={14} />
                  </span>
                  <span>{comment.likes}</span>
                </div>
              )}
            </div>

            {/* Options menu trigger */}
            {isOwn && (
              <div ref={menuRef} className="relative self-center opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
                <button
                  onClick={() => setShowMenu(p => !p)}
                  className="w-7 h-7 rounded-full hover:bg-[#E4E6EB] flex items-center justify-center text-[#65676B] transition-colors"
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z"/></svg>
                </button>
                {showMenu && (
                  <div className="absolute left-full top-0 ml-1 bg-white rounded-xl shadow-xl border border-[#E4E6EB] w-44 z-20 py-1 overflow-hidden">
                    <button
                      onClick={() => { setEditing(true); setShowMenu(false); }}
                      className="flex items-center gap-2 w-full px-3 py-2 hover:bg-[#F0F2F5] text-sm text-[#1C1E21] font-medium transition-colors"
                    >
                      <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20"><path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/></svg>
                      Chỉnh sửa
                    </button>
                    <button
                      onClick={() => { deleteComment(comment.id); setShowMenu(false); }}
                      className="flex items-center gap-2 w-full px-3 py-2 hover:bg-[#F0F2F5] text-sm text-red-600 font-medium transition-colors"
                    >
                      <svg className="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd"/></svg>
                      Xóa
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Meta actions */}
        {!editing && (
          <div className="flex items-center gap-3 mt-0.5 ml-2 relative">
            {/* Reaction button with mini picker */}
            <div
              className="relative flex items-center"
              onMouseEnter={() => { if (reactionTimer.current) clearTimeout(reactionTimer.current); reactionTimer.current = setTimeout(() => setShowReactionPicker(true), 500); }}
              onMouseLeave={() => { if (reactionTimer.current) clearTimeout(reactionTimer.current); reactionTimer.current = setTimeout(() => setShowReactionPicker(false), 500); }}
            >
              {/* Reaction floating picker */}
              {showReactionPicker && (
                <div className="absolute bottom-[120%] left-[-10px] w-max pb-1 z-30">
                  <div className="bg-white rounded-full shadow-[0_2px_12px_rgba(0,0,0,0.2)] border border-[#E4E6EB] px-2 py-1.5 flex gap-1">
                    {REACTIONS.map(r => (
                      <button
                        key={r.key}
                        onClick={(e) => { e.stopPropagation(); pickCommentReaction(r.key); }}
                        title={r.label}
                        className="flex flex-col items-center gap-0.5 group/r hover:bg-transparent"
                      >
                        <span className="text-2xl transform group-hover/r:scale-125 group-hover/r:-translate-y-2 transition-all duration-200 origin-bottom">
                          <Emoji unified={r.unified} emojiStyle={EmojiStyle.FACEBOOK} size={28} />
                        </span>
                        <span className="absolute -top-7 text-[10px] font-bold text-white bg-black/75 px-1.5 py-0.5 rounded-full opacity-0 group-hover/r:opacity-100 transition-opacity whitespace-nowrap">
                          {r.label}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
              <button
                onClick={handleCommentLikeClick}
                className={`text-xs font-bold transition-colors ${currentReaction ? currentReaction.color : "text-[#65676B] hover:text-[#1C1E21]"}`}
              >
                {currentReaction ? currentReaction.label : "Thích"}
              </button>
            </div>
            {depth === 0 && (
              <button
                onClick={() => setShowReplyBox(p => !p)}
                className="text-xs font-bold text-[#65676B] hover:text-[#1C1E21] transition-colors"
              >
                Phản hồi
              </button>
            )}
            <span className="text-xs text-[#65676B]">{comment.time}</span>
          </div>
        )}

        {/* View replies toggle */}
        {depth === 0 && replies.length > 0 && (
          <button
            onClick={() => setShowReplies(p => !p)}
            className="flex items-center gap-1 mt-1 ml-2 text-xs font-bold text-[#65676B] hover:text-[#1C1E21] transition-colors"
          >
            <svg className={`w-3 h-3 transition-transform ${showReplies ? "rotate-180" : ""}`} fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"/></svg>
            {showReplies ? "Ẩn câu trả lời" : `Xem ${replies.length} câu trả lời`}
          </button>
        )}

        {/* Replies */}
        {depth === 0 && showReplies && replies.map(reply => (
          <CommentItem key={reply.id} comment={reply} postId={postId} depth={1} />
        ))}

        {/* Reply input */}
        {showReplyBox && (
          <div className="flex items-center gap-2 mt-2 ml-10">
            <img src={profile.avatar} alt="me" className="w-7 h-7 rounded-full object-cover flex-shrink-0" />
            <div className="flex-1 flex items-center bg-[#F0F2F5] rounded-full px-3 h-8 gap-2">
              <input
                ref={replyRef}
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                placeholder={`Phản hồi ${comment.user}...`}
                className="flex-1 bg-transparent outline-none text-xs text-[#1C1E21] placeholder-[#65676B]"
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleReply();
                  if (e.key === "Escape") { setShowReplyBox(false); setReplyText(""); }
                }}
              />
              <div className="flex items-center gap-1 flex-shrink-0 relative" ref={replyEmojiRef}>
                <button 
                  onClick={() => setShowReplyEmoji(p => !p)}
                  className={`transition-colors ${showReplyEmoji ? "text-[#1877F2]" : "text-[#65676B] hover:text-[#1877F2]"}`} 
                  title="Emoji"
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>
                </button>
                {showReplyEmoji && (
                  <div className="absolute right-0 bottom-full mb-2 z-50 shadow-2xl rounded-xl overflow-hidden border border-[#CED0D4]">
                    <EmojiPicker
                      onEmojiClick={(e) => setReplyText(prev => prev + e.emoji)}
                      emojiStyle={EmojiStyle.FACEBOOK}
                      theme={Theme.LIGHT}
                      searchPlaceholder="Tìm kiếm..."
                      previewConfig={{ showPreview: false }}
                      skinTonesDisabled={true}
                      width={300}
                      height={320}
                    />
                  </div>
                )}
                {replyText && (
                  <button onClick={handleReply} className="text-[#1877F2] flex-shrink-0">
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/></svg>
                  </button>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Comment section ───────────────────────────────────────────────────────────

interface CommentSectionProps {
  postId: string;
  initialVisible?: boolean;
  isModal?: boolean;
}

export default function CommentSection({ postId, initialVisible = false, isModal = false }: CommentSectionProps) {
  const { getPostComments, createComment, fetchComments } = useCommentStore();
  const { profile } = useUserStore();

  const [text, setText] = useState("");
  const [visible, setVisible] = useState(initialVisible);
  const [showAll, setShowAll] = useState(isModal);
  // Track comments created in this specific feed component instance
  const [localCommentIds, setLocalCommentIds] = useState<Set<number | string>>(new Set());
  const [filter, setFilter] = useState<"relevant" | "all" | "newest">("all");
  const [showFilterDropdown, setShowFilterDropdown] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const filterRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const emojiRef = useRef<HTMLDivElement>(null);
  
  useOutsideClick(emojiRef, () => setShowEmojiPicker(false));

  useEffect(() => {
    if (initialVisible) {
      setVisible(true);
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [initialVisible]);

  useEffect(() => {
    if (visible && isModal) {
      fetchComments(postId);
    }
  }, [visible, isModal, postId, fetchComments]);

  // Close filter dropdown on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (filterRef.current && !filterRef.current.contains(e.target as Node)) {
        setShowFilterDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const topComments = getPostComments(postId);
  
  // Apply filter logic
  const isEmojiOnly = (content: string) => {
    const stripped = content.replace(/\s/g, "");
    const emojiRegex = /^(?:\p{Emoji_Presentation}|\p{Extended_Pictographic})+$/u;
    return emojiRegex.test(stripped) && stripped.length <= 8;
  };

  const filteredComments = (() => {
    if (!isModal) {
      // Feed mode: only show locally created comments
      return topComments.filter(c => localCommentIds.has(c.id));
    }
    let result = [...topComments];
    // Default sort oldest-first
    result.sort((a, b) => a.timestamp - b.timestamp);
    
    switch (filter) {
      case "relevant":
        // Hide emoji-only comments unless it's mine
        result = result.filter(c => 
          c.user === profile.name || !isEmojiOnly(c.content)
        );
        break;
      case "newest":
        // Newest first
        result.sort((a, b) => b.timestamp - a.timestamp);
        break;
      case "all":
      default:
        break;
    }
    return result;
  })();

  const PREVIEW_COUNT = 2;
  const displayed = (showAll || !isModal) ? filteredComments : filteredComments.slice(-PREVIEW_COUNT);

  const FILTER_LABELS: Record<string, string> = {
    relevant: "Phù hợp nhất",
    all: "Tất cả bình luận",
    newest: "Mới nhất",
  };

  const FILTER_DESCRIPTIONS: Record<string, string> = {
    relevant: "Ẩn bình luận chỉ có icon, ưu tiên của bạn",
    all: "Hiển thị tất cả bình luận",
    newest: "Bình luận mới nhất hiện trước",
  };

  async function handleSubmit() {
    if (!text.trim()) return;
    const tempText = text;
    setText("");
    
    const newId = await createComment(postId, tempText, null);
    
    setVisible(true);
    setShowAll(true);
    
    if (newId && !isModal) {
      setLocalCommentIds(prev => new Set(prev).add(newId));
    }
  }

  function focusInput() {
    setVisible(true);
    setTimeout(() => inputRef.current?.focus(), 50);
  }

  return (
    <div>
      {/* Filter bar - only in modal */}
      {isModal && topComments.length > 0 && (
        <div className="px-4 py-2 flex items-center justify-start">
          <div className="relative" ref={filterRef}>
            <button
              onClick={() => setShowFilterDropdown(p => !p)}
              className="flex items-center gap-1 text-[14px] font-semibold text-[#65676B] hover:text-[#1C1E21] transition-colors"
            >
              {FILTER_LABELS[filter]}
              <svg className={`w-3 h-3 transition-transform ${showFilterDropdown ? "rotate-180" : ""}`} fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"/>
              </svg>
            </button>
            {showFilterDropdown && (
              <div className="absolute left-0 top-full mt-1 w-56 bg-white rounded-lg shadow-lg border border-[#E4E6EB] py-1 z-50">
                {(["relevant", "all", "newest"] as const).map(key => (
                  <button
                    key={key}
                    onClick={() => { setFilter(key); setShowFilterDropdown(false); }}
                    className={`w-full text-left px-3 py-2 hover:bg-[#F0F2F5] transition-colors ${filter === key ? "bg-[#E7F3FF]" : ""}`}
                  >
                    <div className="text-[13px] font-semibold text-[#1C1E21] flex items-center gap-2">
                      {FILTER_LABELS[key]}
                      {filter === key && (
                        <svg className="w-4 h-4 text-[#1877F2]" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd"/>
                        </svg>
                      )}
                    </div>
                    <div className="text-[11px] text-[#65676B]">
                      {FILTER_DESCRIPTIONS[key]}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Comments list */}
      {visible && displayed.length > 0 && (
        <div className="px-4 pb-2">
          {isModal && !showAll && filteredComments.length > PREVIEW_COUNT && (
            <button
              onClick={() => setShowAll(true)}
              className="text-sm font-bold text-[#65676B] hover:text-[#1C1E21] transition-colors mt-1"
            >
              Xem thêm {filteredComments.length - PREVIEW_COUNT} bình luận
            </button>
          )}
          {displayed.map(c => (
            <CommentItem key={c.id} comment={c} postId={postId} depth={0} />
          ))}
          {isModal && showAll && filteredComments.length > PREVIEW_COUNT && (
            <button
              onClick={() => setShowAll(false)}
              className="text-sm font-bold text-[#65676B] hover:text-[#1C1E21] transition-colors mt-2 ml-10"
            >
              Ẩn bớt bình luận
            </button>
          )}
        </div>
      )}

      {/* New comment input */}
      <div className="px-4 pb-3 flex items-center gap-2">
        <img src={profile.avatar} alt="me" className="w-8 h-8 rounded-full object-cover flex-shrink-0" />
        <div className="flex-1 flex items-center bg-[#F0F2F5] hover:bg-[#E4E6EB] rounded-full px-4 h-9 gap-2 transition-colors">
          <input
            ref={inputRef}
            value={text}
            onChange={(e) => setText(e.target.value)}
            onClick={() => setVisible(true)}
            placeholder="Viết bình luận..."
            className="flex-1 bg-transparent outline-none text-sm text-[#1C1E21] placeholder-[#65676B]"
            onKeyDown={(e) => { if (e.key === "Enter") handleSubmit(); }}
          />
          <div className="flex items-center gap-1 flex-shrink-0 relative" ref={emojiRef}>
            <button 
              onClick={() => setShowEmojiPicker(p => !p)}
              className={`transition-colors ${showEmojiPicker ? "text-[#1877F2]" : "text-[#65676B] hover:text-[#1877F2]"}`} 
              title="Emoji"
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>
            </button>
            {showEmojiPicker && (
              <div className="absolute right-0 bottom-full mb-2 z-50 shadow-2xl rounded-xl overflow-hidden border border-[#CED0D4]">
                <EmojiPicker
                  onEmojiClick={(e) => setText(prev => prev + e.emoji)}
                  emojiStyle={EmojiStyle.FACEBOOK}
                  theme={Theme.LIGHT}
                  searchPlaceholder="Tìm kiếm..."
                  previewConfig={{ showPreview: false }}
                  skinTonesDisabled={true}
                  width={300}
                  height={320}
                />
              </div>
            )}
            {text && (
              <button onClick={handleSubmit} className="text-[#1877F2]">
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/></svg>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );

  // expose focusInput for Post component
  void focusInput;
}
