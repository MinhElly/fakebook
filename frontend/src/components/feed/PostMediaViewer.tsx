import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import type { Post as PostType } from "@/types";
import { getMediaUrl } from "@/services/mediaService";
import MediaImage from "./MediaImage";
import MediaVideo from "./MediaVideo";
import Post from "./Post";
import type { ReactionSummary } from "@/services/reactionService";

interface Props {
  post: PostType;
  initialIndex: number;
  onClose: () => void;
  onReactionChange: (summary: ReactionSummary) => void;
}

export default function PostMediaViewer({ post, initialIndex, onClose, onReactionChange }: Props) {
  const media = (post.mediaIds?.length ? post.mediaIds : post.image ? [post.image] : []).map(getMediaUrl);
  const [index, setIndex] = useState(Math.min(initialIndex, media.length - 1));
  const currentIndex = Math.max(0, Math.min(index, media.length - 1));
  const [showComments, setShowComments] = useState(true);
  const isVideo = post.media?.find(item => item.id === post.mediaIds?.[currentIndex])?.mediaType === "VIDEO";
  const dialog = useRef<HTMLDivElement>(null);
  const close = useRef<HTMLButtonElement>(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    close.current?.focus();
    function handleKey(event: KeyboardEvent) {
      if (event.key === "Escape") { event.preventDefault(); event.stopPropagation(); closeRef.current(); return; }
      if (!media.length) return;
      const target = event.target as HTMLElement;
      if (target.matches("input, textarea, select, video, [contenteditable=true]")) return;
      if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
        event.preventDefault();
        setIndex(current => (current + (event.key === "ArrowLeft" ? -1 : 1) + media.length) % media.length);
      }
      if (event.key === "Tab") {
        const controls = Array.from(dialog.current?.querySelectorAll<HTMLElement>("button, a[href], input, textarea, select, [tabindex='0']") ?? []).filter(el => !el.hasAttribute("disabled") && el.getClientRects().length);
        const first = controls[0], last = controls[controls.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    }
    document.addEventListener("keydown", handleKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKey);
      previous?.focus();
    };
  }, [media.length]);

  if (!media.length) return null;
  return createPortal(
    <div ref={dialog} role="dialog" aria-modal="true" aria-label={`Ảnh và video của ${post.user}`}
      className={`fixed inset-0 z-[60] grid bg-black ${showComments ? "grid-rows-[minmax(0,45fr)_minmax(0,55fr)] md:grid-cols-[minmax(0,1fr)_360px] md:grid-rows-1" : "grid-cols-1 grid-rows-1"}`}>
      <div className="relative flex min-h-0 min-w-0 items-center justify-center overflow-hidden bg-black p-0" data-media-stage>
        <button ref={close} type="button" aria-label="Đóng trình xem" onClick={onClose}
          className="absolute left-3 top-3 z-10 flex size-10 items-center justify-center rounded-full bg-[#E4E6EB] text-[#1C1E21] hover:bg-white focus-visible:outline-2 focus-visible:outline-white">
          <svg aria-hidden="true" className="size-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m6 6 12 12M6 18 18 6" /></svg>
        </button>
        {isVideo
          ? <MediaVideo key={media[currentIndex]} mediaId={post.mediaIds![currentIndex]} autoPlayOnVisible className="h-full w-full" />
          : <MediaImage key={media[currentIndex]} src={media[currentIndex]} alt={`Ảnh ${currentIndex + 1} trong ${media.length}`}
            draggable={false} className="h-full w-full rounded-none object-contain" />}
        <button type="button" aria-label={showComments ? "Ẩn bình luận" : "Hiện bình luận"} aria-expanded={showComments}
          onClick={() => setShowComments(value => !value)} className="absolute right-3 top-3 z-10 rounded-full bg-white/15 p-3 text-white hover:bg-white/25">
          <svg aria-hidden="true" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M16 3v18"/></svg>
        </button>
        {isVideo && !showComments && <div className="pointer-events-none absolute bottom-16 left-5 max-w-[75%] bg-black/40 px-3 py-2 text-sm text-white"><strong>{post.user}</strong><p className="line-clamp-2">{post.content}</p></div>}
        {media.length > 1 && <>
          <span className="absolute right-4 top-4 rounded bg-black/60 px-2 py-1 text-sm text-white" aria-live="polite">{currentIndex + 1} / {media.length}</span>
          <button type="button" aria-label="Tệp trước" onClick={() => setIndex((currentIndex - 1 + media.length) % media.length)}
            className="absolute left-3 flex size-11 items-center justify-center rounded-full bg-white/80 text-black hover:bg-white focus-visible:outline-2 focus-visible:outline-white">
            <svg aria-hidden="true" className="size-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m15 5-7 7 7 7" /></svg>
          </button>
          <button type="button" aria-label="Tệp tiếp theo" onClick={() => setIndex((currentIndex + 1) % media.length)}
            className="absolute right-3 flex size-11 items-center justify-center rounded-full bg-white/80 text-black hover:bg-white focus-visible:outline-2 focus-visible:outline-white">
            <svg aria-hidden="true" className="size-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m9 5 7 7-7 7" /></svg>
          </button>
        </>}
      </div>
      {showComments && <aside className="min-h-0 overflow-y-auto overscroll-contain bg-white" aria-label="Bài viết và bình luận">
        <Post post={post} isModal presentation="media" onReactionChange={onReactionChange} />
      </aside>}
    </div>, document.body,
  );
}
