import { useState, useRef, useEffect } from "react";
import { useCommentStore } from "@/stores/commentStore";
import { useUserStore } from "@/stores/userStore";
import { useOutsideClick } from "@/hooks/useOutsideClick";
import type { Comment } from "@/types";

const REACTIONS = [
  { key: "like",  emoji: "👍", label: "Thích",     color: "text-[#1877F2]" },
  { key: "love",  emoji: "❤️", label: "Yêu thích", color: "text-red-500" },
  { key: "haha",  emoji: "😂", label: "Haha",       color: "text-yellow-400" },
  { key: "wow",   emoji: "😮", label: "Wow",        color: "text-yellow-400" },
  { key: "sad",   emoji: "😢", label: "Buồn",       color: "text-yellow-400" },
  { key: "angry", emoji: "😡", label: "Phẫn nộ",   color: "text-orange-500" },
];

// ─── Single comment item ───────────────────────────────────────────────────────

interface CommentItemProps {
  comment: Comment;
  postId: number;
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
  const reactionTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

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
    <div className={`flex gap-2 ${depth > 0 ? "ml-10 mt-2" : "mt-3"}`}>
      <img src={comment.avatar} alt={comment.user} className={`rounded-full object-cover flex-shrink-0 ${depth > 0 ? "w-7 h-7" : "w-8 h-8"}`} />

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
            <div className="bg-[#F0F2F5] rounded-2xl px-3 py-2 inline-block max-w-full">
              <p className="font-semibold text-[#1C1E21] text-xs leading-none mb-0.5">{comment.user}</p>
              <p className="text-sm text-[#1C1E21] leading-snug whitespace-pre-wrap break-words">{comment.content}</p>
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
          <div className="flex items-center gap-3 mt-0.5 ml-2">
            {/* Reaction button with mini picker */}
            <div
              className="relative"
              onMouseEnter={() => { reactionTimer.current = setTimeout(() => setShowReactionPicker(true), 400); }}
              onMouseLeave={() => { if (reactionTimer.current) clearTimeout(reactionTimer.current); setShowReactionPicker(false); }}
            >
              {showReactionPicker && (
                <div className="absolute bottom-full left-0 mb-1 bg-white rounded-full shadow-xl border border-[#E4E6EB] px-1.5 py-1 flex gap-0.5 z-30 whitespace-nowrap">
                  {REACTIONS.map(r => (
                    <button key={r.key} onClick={() => pickCommentReaction(r.key)} title={r.label}
                      className="group/r flex flex-col items-center px-0.5">
                      <span className="text-lg transform group-hover/r:scale-125 transition-transform duration-100 origin-bottom">{r.emoji}</span>
                    </button>
                  ))}
                </div>
              )}
              <button
                onClick={handleCommentLikeClick}
                className={`text-xs font-bold transition-colors ${currentReaction ? currentReaction.color : "text-[#65676B] hover:text-[#1C1E21]"}`}
              >
                {currentReaction ? <>{currentReaction.emoji} {currentReaction.label}</> : "Thích"}
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
            {comment.likes > 0 && (
              <span className="flex items-center gap-0.5 text-xs text-[#65676B]">
                <span className="bg-[#1877F2] rounded-full w-4 h-4 inline-flex items-center justify-center text-[9px]">👍</span>
                {comment.likes}
              </span>
            )}
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
              {replyText && (
                <button onClick={handleReply} className="text-[#1877F2] flex-shrink-0">
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/></svg>
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Comment section ───────────────────────────────────────────────────────────

interface CommentSectionProps {
  postId: number;
  initialVisible?: boolean;
}

export default function CommentSection({ postId, initialVisible = false }: CommentSectionProps) {
  const { getPostComments, createComment } = useCommentStore();
  const { profile } = useUserStore();

  const [text, setText] = useState("");
  const [visible, setVisible] = useState(initialVisible);
  const [showAll, setShowAll] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const topComments = getPostComments(postId);
  const PREVIEW_COUNT = 2;
  const displayed = showAll ? topComments : topComments.slice(-PREVIEW_COUNT);

  function handleSubmit() {
    if (!text.trim()) return;
    createComment(postId, text, null);
    setText("");
    setVisible(true);
    setShowAll(true);
  }

  function focusInput() {
    setVisible(true);
    setTimeout(() => inputRef.current?.focus(), 50);
  }

  return (
    <div>
      {/* Toggle & summary row */}
      {topComments.length > 0 && (
        <button
          onClick={() => setVisible(p => !p)}
          className="w-full flex items-center justify-between px-4 py-1.5 text-xs text-[#65676B] hover:bg-[#F0F2F5] transition-colors"
        >
          <span className="font-semibold">{topComments.length} bình luận</span>
          <span className="flex items-center gap-1">
            Sắp xếp theo: Phù hợp nhất
            <svg className={`w-3 h-3 transition-transform ${visible ? "rotate-180" : ""}`} fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"/></svg>
          </span>
        </button>
      )}

      {/* Comments list */}
      {visible && topComments.length > 0 && (
        <div className="px-4 pb-2">
          {!showAll && topComments.length > PREVIEW_COUNT && (
            <button
              onClick={() => setShowAll(true)}
              className="text-sm font-bold text-[#65676B] hover:text-[#1C1E21] transition-colors mt-1"
            >
              Xem thêm {topComments.length - PREVIEW_COUNT} bình luận
            </button>
          )}
          {displayed.map(c => (
            <CommentItem key={c.id} comment={c} postId={postId} depth={0} />
          ))}
          {showAll && topComments.length > PREVIEW_COUNT && (
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
          <div className="flex items-center gap-1 flex-shrink-0">
            <button className="text-[#65676B] hover:text-[#1877F2] transition-colors" title="Emoji">
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>
            </button>
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
