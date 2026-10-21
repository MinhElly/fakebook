import { useEffect, useRef, useState } from "react";
import type { Post } from "@/types";
import { getMediaUrl, uploadPostMedia } from "@/services/mediaService";
import MediaImage from "./MediaImage";

type Attachment = { key: string; mediaId?: string; file?: File; preview?: string; type: string; name: string;
  status: "queued" | "uploading" | "processing" | "ready" | "error" | "cancelled"; percent: number; error?: string };
const allowed = new Set(["image/jpeg", "image/png", "image/webp", "image/gif", "image/avif", "video/mp4", "video/webm", "video/quicktime"]);

export function usePostAttachments(post?: Post) {
  const [items, setItems] = useState<Attachment[]>(() => (post?.mediaIds ?? []).map(id => ({
    key: id, mediaId: id, type: post?.media?.find(media => media.id === id)?.mediaType ?? "IMAGE",
    name: "Tệp đã đăng", status: "ready", percent: 100,
  })));
  const [error, setError] = useState("");
  const currentItems = useRef(items);
  currentItems.current = items;

  const running = useRef(new Map<string, AbortController>());
  const urls = useRef(new Set<string>());
  const mounted = useRef(true);
  const update = (key: string, patch: Partial<Attachment>) => {
    if (mounted.current) setItems(previous => previous.map(item => item.key === key ? { ...item, ...patch } : item));
  };
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; running.current.forEach(controller => controller.abort()); running.current.clear(); urls.current.forEach(URL.revokeObjectURL); urls.current.clear(); };
  }, []);
  async function uploadAll(publicationId?: string): Promise<string[]> {
    const snapshot = currentItems.current;
    let cursor = 0;
    const ids = new Map<string, string>();
    const failures: unknown[] = [];
    await Promise.all(Array.from({ length: Math.min(3, snapshot.length) }, async () => {
      while (cursor < snapshot.length) {
        const item = snapshot[cursor++];
        if (item.mediaId) { ids.set(item.key, item.mediaId); continue; }
        if (!item.file || !mounted.current) { failures.push(new Error("Upload cancelled")); continue; }
        const controller = new AbortController();
        running.current.set(item.key, controller);
        update(item.key, { status: "uploading", error: undefined });
        try {
          const media = await uploadPostMedia(item.file, { publicationId, fileKey: publicationId ? item.key : undefined, signal: controller.signal,
            onProgress: (percent, status) => update(item.key, { percent, status }) });
          ids.set(item.key, media.id);
          update(item.key, { mediaId: media.id, type: media.mediaType ?? item.type, status: "ready", percent: 100 });
        } catch (reason) {
          failures.push(reason);
          update(item.key, { status: controller.signal.aborted ? "cancelled" : "error",
            error: reason instanceof Error ? reason.message : "Upload failed" });
        } finally { running.current.delete(item.key); }
      }
    }));
    if (failures.length || !mounted.current) throw new Error("Media upload incomplete");
    return snapshot.map(item => ids.get(item.key)!);
  }

  function add(files: FileList | File[]) {
    const candidates = Array.from(files);
    const invalid = candidates.some(file => !allowed.has(file.type) || !file.size);
      const previous = currentItems.current;
      const seen = new Set(previous.flatMap(item => item.file ? [`${item.file.name}:${item.file.size}:${item.file.lastModified}`] : []));
      const accepted = candidates.filter(file => {
        if (!allowed.has(file.type) || !file.size) return false;
        const signature = `${file.name}:${file.size}:${file.lastModified}`;
        if (seen.has(signature)) return false; seen.add(signature); return true;
      });
      if (previous.length + accepted.length > 30) { setError("Mỗi bài có tối đa 30 ảnh/video. Hãy chọn ít tệp hơn."); return; }
      setError(invalid ? "Một số tệp bị bỏ qua. Hỗ trợ JPG, PNG, WebP, GIF, AVIF, MP4, WebM và MOV." : "");
      const next: Attachment[] = [...previous, ...accepted.map(file => {
        const preview = URL.createObjectURL(file); urls.current.add(preview);
        return { key: crypto.randomUUID(), file, preview, type: file.type.startsWith("video/") ? "VIDEO" : "IMAGE", name: file.name, status: "queued" as const, percent: 0 };
      })];
      currentItems.current = next; setItems(next);
  }
  function remove(key: string) {
    running.current.get(key)?.abort();
    setItems(previous => {
      const removed = previous.find(item => item.key === key);
      if (removed?.preview) { URL.revokeObjectURL(removed.preview); urls.current.delete(removed.preview); }
      return previous.filter(item => item.key !== key);
    });
  }
  function move(key: string, position: number) {
    setItems(previous => {
      const source = previous.findIndex(item => item.key === key);
      if (source < 0 || position < 0 || position >= previous.length) return previous;
      const next = [...previous]; const [item] = next.splice(source, 1); next.splice(position, 0, item); return next;
    });
  }
  function retry(key: string) { if (!running.current.has(key)) update(key, { status: "queued", error: undefined }); }
  function resetUploads() {
    const next = currentItems.current.map(item => item.file ? { ...item, mediaId: undefined, status: "queued" as const, percent: 0, error: undefined } : item);
    currentItems.current = next; setItems(next);
  }
  return { items, error, add, remove, move, retry, uploadAll, resetUploads, cancel: (key: string) => running.current.get(key)?.abort(),
    ready: !items.some(item => item.status === "uploading" || item.status === "processing"), mediaIds: items.flatMap(item => item.mediaId ? [item.mediaId] : []) };
}

export default function PostAttachments({ manager, disabled }: { manager: ReturnType<typeof usePostAttachments>; disabled: boolean }) {
  const input = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [editing, setEditing] = useState(false);
  return <section aria-label="Ảnh và video đính kèm" className={`relative mx-4 mb-3 overflow-hidden rounded-lg border ${dragging ? "border-[#1877F2] bg-[#E7F3FF]" : "border-[#CED0D4]"}`}
    onDragOver={event => { if (event.dataTransfer.types.includes("Files")) { event.preventDefault(); setDragging(true); } }}
    onDragLeave={event => { if (!(event.relatedTarget instanceof Node) || !event.currentTarget.contains(event.relatedTarget)) setDragging(false); }}
    onDrop={event => { event.preventDefault(); event.stopPropagation(); setDragging(false); if (!disabled && event.dataTransfer.files.length) manager.add(event.dataTransfer.files); }}>
    <div className={`flex items-center gap-2 p-3 text-sm ${manager.items.length && !editing ? "absolute left-0 top-0 z-10" : "justify-between"}`}><button type="button" disabled={disabled} onClick={() => setEditing(!editing)} className="rounded-md bg-white px-3 py-2 font-semibold shadow-sm">✎ {editing ? "Xong" : "Chỉnh sửa"}</button>{editing && <span>{manager.items.length}/30</span>}
      <button type="button" disabled={disabled || manager.items.length === 30} onClick={() => input.current?.click()} className={`rounded-md bg-[#E7F3FF] px-3 py-2 font-semibold text-[#1877F2] disabled:opacity-50 ${manager.items.length && !editing ? "hidden" : ""}`}>Thêm tệp</button></div>
    <input ref={input} aria-label="Chọn ảnh hoặc video" className="hidden" type="file" multiple accept={[...allowed].join(",")} disabled={disabled}
      onChange={event => { if (event.target.files) manager.add(event.target.files); event.target.value = ""; }} />
    {!manager.items.length && <button type="button" disabled={disabled} onClick={() => input.current?.click()} className="flex min-h-28 w-full flex-col items-center justify-center gap-1 bg-[#F0F2F5] p-4 text-sm"><strong>Thêm ảnh hoặc video</strong><span className="text-[#65676B]">Chọn nhiều tệp hoặc kéo thả vào đây</span></button>}
    <ol className={`grid gap-1 ${manager.items.length > 1 ? "grid-cols-2" : "grid-cols-1"}`}>{manager.items.map((item, index) => <li key={item.key} draggable={!disabled}
      onDragStart={event => event.dataTransfer.setData("application/x-fakebook-attachment", item.key)}
      onDragOver={event => { if (event.dataTransfer.types.includes("application/x-fakebook-attachment")) event.preventDefault(); }}
      onDrop={event => { const key = event.dataTransfer.getData("application/x-fakebook-attachment"); if (key) { event.preventDefault(); event.stopPropagation(); if (!disabled) manager.move(key, index); } }}
      className={`min-w-0 overflow-hidden bg-[#F0F2F5] ${manager.items.length > 1 && manager.items.length % 2 === 1 && index === 0 ? "col-span-2" : ""}`}>
      <div className={`relative overflow-hidden bg-black/5 ${manager.items.length === 1 ? "" : "aspect-square"}`}>
        {item.type === "VIDEO" ? item.preview ? <video src={item.preview} controls playsInline preload="metadata" draggable={false} className={manager.items.length === 1 ? "max-h-[470px] w-full object-contain" : "h-full w-full object-cover"} /> : <MediaImage src={`${getMediaUrl(item.mediaId!)}?poster=true`} alt="Video đã đăng" className={manager.items.length === 1 ? "max-h-[470px] w-full object-contain" : "h-full w-full object-cover"} />
          : <MediaImage src={item.preview ?? getMediaUrl(item.mediaId!)} alt={item.name} draggable={false} className={manager.items.length === 1 ? "max-h-[470px] w-full object-contain" : "h-full w-full object-cover"} />}
        <button type="button" aria-label={`Xóa tệp ${index + 1}`} disabled={disabled} onClick={() => manager.remove(item.key)} className="absolute right-1 top-1 size-7 rounded-full bg-white text-lg text-black">×</button>
        {item.type === "VIDEO" && <span className="absolute bottom-1 left-1 bg-black/70 px-1 text-xs text-white">Video</span>}
      </div>
      <div className="space-y-1 text-xs">{editing && <p className="truncate px-2" title={item.name}>{item.name}</p>}
        <p aria-live="polite" className={item.status === "queued" || item.status === "ready" ? "sr-only" : "px-2 py-1"}>{item.status === "ready" ? "Sẵn sàng" : item.status === "processing" ? "Đang xử lý…" : item.status === "queued" ? "Đang chờ…" : item.status === "uploading" ? `Đang tải ${item.percent}%` : item.status === "cancelled" ? "Đã hủy" : "Không tải được tệp"}</p>
        {(item.status === "uploading" || item.status === "processing") && <><progress aria-label={`Tiến độ ${item.name}`} value={item.percent} max={100} className="w-full" /><button type="button" onClick={() => manager.cancel(item.key)} className="text-[#1877F2]">Hủy tải</button></>}
        {(item.status === "error" || item.status === "cancelled") && <button type="button" onClick={() => manager.retry(item.key)} title={item.error} className="font-semibold text-[#1877F2]">Thử lại</button>}
        {editing && <div className="flex justify-between gap-1"><button type="button" aria-label={`Di chuyển tệp ${index + 1} lên trước`} disabled={disabled || index === 0} onClick={() => manager.move(item.key, index - 1)} className="px-2 py-1 disabled:opacity-30">←</button>
          <button type="button" aria-label={`Di chuyển tệp ${index + 1} ra sau`} disabled={disabled || index === manager.items.length - 1} onClick={() => manager.move(item.key, index + 1)} className="px-2 py-1 disabled:opacity-30">→</button></div>}
      </div>
    </li>)}</ol>
    {manager.error && <p role="alert" className="mt-2 text-sm text-red-600">{manager.error}</p>}
  </section>;
}
