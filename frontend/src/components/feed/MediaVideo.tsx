import { useEffect, useRef, useState } from "react";
import api from "@/services/apis";
import { resolveApiUrl } from "@/config/runtime-config";

export default function MediaVideo({ mediaId, className = "", autoPlayOnVisible = false, onExpand }: { mediaId: string; className?: string; autoPlayOnVisible?: boolean; onExpand?: () => void }) {
  const [source, setSource] = useState("");
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const player = useRef<HTMLVideoElement>(null);
  const position = useRef(0);
  const container = useRef<HTMLDivElement>(null);
  const visible = useRef(false);
  useEffect(() => { position.current = 0; }, [mediaId]);
  useEffect(() => {
    if (!autoPlayOnVisible || !source || error || !container.current) return;
    const observer = new IntersectionObserver(entries => {
      visible.current = entries[0].intersectionRatio >= 0.6;
      const video = player.current;
      if (!video) return;
      if (visible.current && !document.hidden) void video.play().catch(() => {});
      else video.pause();
    }, { threshold: [0, 0.6, 1] });
    observer.observe(container.current);
    const visibility = () => { if (document.hidden) player.current?.pause(); else if (visible.current) void player.current?.play().catch(() => {}); };
    document.addEventListener("visibilitychange", visibility);
    return () => { observer.disconnect(); document.removeEventListener("visibilitychange", visibility); };
  }, [source, error, autoPlayOnVisible]);
  useEffect(() => {
    const controller = new AbortController();
    let ticket: string | undefined;
    let timer: ReturnType<typeof setTimeout> | undefined;
    setSource(""); setError(false);
    async function refresh() {
      try {
        const { data } = await api.post<{ ticket: string; path: string }>(`/services/mediaservice/api/media/${mediaId}/playback`, { ticket }, { signal: controller.signal });
        if (controller.signal.aborted) return;
        ticket = data.ticket;
        setSource(resolveApiUrl(data.path));
        timer = setTimeout(refresh, 30000);
      } catch {
        if (!controller.signal.aborted) { setError(true); player.current?.pause(); player.current?.removeAttribute("src"); player.current?.load(); }
      }
    }
    void refresh();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [mediaId, attempt]);
  return <div ref={container} className={`relative min-w-0 overflow-hidden flex items-center justify-center bg-black ${className}`}>
    {source && !error && <video ref={player} src={source} controls playsInline muted={autoPlayOnVisible} preload="metadata"
      onPlay={() => { document.querySelectorAll("video").forEach(video => { if (video !== player.current) video.pause(); }); }}
      aria-label="Video bài viết" className="block h-full max-h-full w-full min-w-0 object-contain" onTimeUpdate={() => { position.current = player.current?.currentTime ?? 0; }}
      onLoadedMetadata={() => { if (player.current && position.current) player.current.currentTime = Math.min(position.current, player.current.duration || 0); }}
      onError={() => setError(true)} />}
    {onExpand && <button type="button" aria-label="Mở trình xem video" onClick={() => { player.current?.pause(); onExpand(); }}
      className="absolute right-3 top-3 rounded-full bg-black/60 p-2 text-white hover:bg-black/80 focus-visible:outline-2 focus-visible:outline-white">
      <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5" /></svg>
    </button>}
    {!source && !error && <span role="status" className="p-8 text-sm text-white">Đang tải video…</span>}
    {error && <div className="p-6 text-center text-sm text-white"><p>Không phát được video.</p><button type="button" onClick={() => setAttempt(value => value + 1)} className="mt-2 rounded bg-white px-3 py-2 font-semibold text-black">Thử lại</button></div>}
  </div>;
}
