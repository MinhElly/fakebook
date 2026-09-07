import { useState, useEffect, useRef } from "react";
import { usePostStore } from "@/stores/postStore";
import { useUserStore } from "@/stores/userStore";
import type { Post } from "@/types";

interface Props {
  onClose: () => void;
  editPost?: Post;
}

const EMOJI_QUICK = ["😊", "❤️", "😂", "🔥", "👍", "🎉", "😍", "🙏"];

const BG_COLORS = [
  null,
  "linear-gradient(135deg,#1877F2,#42B72A)",
  "linear-gradient(135deg,#F5533D,#FF9A00)",
  "linear-gradient(135deg,#833AB4,#FD1D1D,#FCB045)",
  "linear-gradient(135deg,#0F2027,#203A43,#2C5364)",
  "linear-gradient(135deg,#11998e,#38ef7d)",
];

export default function CreatePostModal({ onClose, editPost }: Props) {
  const { addPost, updatePost } = usePostStore();
  const { profile } = useUserStore();

  const [content, setContent] = useState(editPost?.content ?? "");
  const [imageUrl, setImageUrl] = useState(editPost?.image ?? "");
  const [showImageInput, setShowImageInput] = useState(!!editPost?.image);
  const [bgGradient, setBgGradient] = useState<string | null>(null);
  const [showEmoji, setShowEmoji] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const isEdit = !!editPost;
  const canSubmit = content.trim().length > 0;

  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  function handleSubmit() {
    if (!canSubmit) return;
    const image = imageUrl.trim() || null;
    if (isEdit) {
      updatePost(editPost!.id, content.trim(), image);
    } else {
      addPost(content.trim(), image);
    }
    onClose();
  }

  function insertEmoji(emoji: string) {
    const el = textareaRef.current;
    if (!el) return;
    const start = el.selectionStart;
    const end = el.selectionEnd;
    const newVal = content.slice(0, start) + emoji + content.slice(end);
    setContent(newVal);
    setTimeout(() => {
      el.selectionStart = el.selectionEnd = start + emoji.length;
      el.focus();
    }, 0);
    setShowEmoji(false);
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      {/* Modal */}
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-[520px] mx-4 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-[#E4E6EB]">
          <div className="w-9" />
          <h2 className="font-bold text-[#1C1E21] text-lg">
            {isEdit ? "Chỉnh sửa bài viết" : "Tạo bài viết"}
          </h2>
          <button
            onClick={onClose}
            className="w-9 h-9 rounded-full bg-[#E4E6EB] hover:bg-[#D8DADF] flex items-center justify-center transition-colors text-[#1C1E21]"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd"/>
            </svg>
          </button>
        </div>

        {/* Author */}
        <div className="flex items-center gap-3 px-4 pt-3 pb-1">
          <img src={profile.avatar} alt="me" className="w-10 h-10 rounded-full object-cover" />
          <div>
            <p className="font-semibold text-[#1C1E21] text-sm">{profile.name}</p>
            <button className="flex items-center gap-1 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-xs font-semibold px-2 py-0.5 rounded-md transition-colors">
              <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"/></svg>
              Bạn bè
              <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"/></svg>
            </button>
          </div>
        </div>

        {/* Text area */}
        <div
          className="mx-4 rounded-xl overflow-hidden mb-2"
          style={bgGradient ? { background: bgGradient } : {}}
        >
          <textarea
            ref={textareaRef}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={`${profile.name.split(" ").pop()} ơi, bạn đang nghĩ gì thế?`}
            rows={bgGradient ? 4 : 3}
            className={`w-full resize-none outline-none text-[#1C1E21] placeholder-[#65676B] leading-relaxed ${
              bgGradient
                ? "bg-transparent text-white placeholder-white/70 text-xl font-semibold text-center p-6"
                : "bg-transparent text-base p-2"
            }`}
          />
        </div>

        {/* Background color picker */}
        {!showImageInput && (
          <div className="px-4 mb-2 flex items-center gap-2">
            {BG_COLORS.map((bg, i) => (
              <button
                key={i}
                onClick={() => setBgGradient(bg === bgGradient ? null : bg)}
                className={`w-8 h-8 rounded-full border-2 transition-transform hover:scale-110 ${bgGradient === bg ? "border-[#1877F2] scale-110" : "border-transparent"}`}
                style={{ background: bg ?? "#E4E6EB" }}
                title={bg ? "Nền màu" : "Không nền"}
              >
                {!bg && <span className="text-xs text-[#65676B] flex items-center justify-center h-full font-bold">A</span>}
              </button>
            ))}
          </div>
        )}

        {/* Image URL input */}
        {showImageInput && (
          <div className="mx-4 mb-2">
            <div className="border-2 border-dashed border-[#CED0D4] rounded-xl p-3">
              <input
                type="text"
                placeholder="Dán URL ảnh vào đây..."
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
                className="w-full outline-none text-sm text-[#1C1E21] placeholder-[#65676B]"
              />
              {imageUrl && (
                <img
                  src={imageUrl}
                  alt="preview"
                  className="mt-2 w-full max-h-48 object-cover rounded-lg"
                  onError={(e) => (e.currentTarget.style.display = "none")}
                  onLoad={(e) => (e.currentTarget.style.display = "block")}
                />
              )}
            </div>
          </div>
        )}

        {/* Emoji picker quick */}
        {showEmoji && (
          <div className="mx-4 mb-2 bg-[#F0F2F5] rounded-xl p-3 flex flex-wrap gap-2">
            {EMOJI_QUICK.map((em) => (
              <button key={em} onClick={() => insertEmoji(em)} className="text-2xl hover:scale-125 transition-transform">
                {em}
              </button>
            ))}
          </div>
        )}

        {/* Toolbar */}
        <div className="mx-4 mb-3 border border-[#CED0D4] rounded-xl px-3 py-2 flex items-center justify-between">
          <span className="text-sm font-semibold text-[#1C1E21]">Thêm vào bài viết của bạn</span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => { setShowImageInput(p => !p); setBgGradient(null); }}
              className={`w-9 h-9 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors ${showImageInput ? "bg-[#E7F3FF]" : ""}`}
              title="Ảnh/Video"
            >
              <svg className="w-6 h-6 text-green-500" fill="currentColor" viewBox="0 0 24 24"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
            </button>
            <button
              onClick={() => setShowEmoji(p => !p)}
              className={`w-9 h-9 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors ${showEmoji ? "bg-[#E7F3FF]" : ""}`}
              title="Cảm xúc"
            >
              <svg className="w-6 h-6 text-yellow-400" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>
            </button>
            <button className="w-9 h-9 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors" title="Tag bạn bè">
              <svg className="w-6 h-6 text-blue-500" fill="currentColor" viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </button>
            <button className="w-9 h-9 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors" title="Check in">
              <svg className="w-6 h-6 text-red-500" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
            </button>
          </div>
        </div>

        {/* Submit */}
        <div className="px-4 pb-4">
          <button
            onClick={handleSubmit}
            disabled={!canSubmit}
            className={`w-full h-10 rounded-lg font-bold text-sm transition-colors ${
              canSubmit
                ? "bg-[#1877F2] hover:bg-[#166FE5] text-white"
                : "bg-[#E4E6EB] text-[#BCC0C4] cursor-not-allowed"
            }`}
          >
            {isEdit ? "Lưu" : "Đăng"}
          </button>
        </div>
      </div>
    </div>
  );
}
