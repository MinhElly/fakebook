import api from "./apis";
import axios from "axios";
import { resolveApiUrl } from "@/config/runtime-config";

export interface MediaDTO {
  id: string;
  ownerId?: string;
  url?: string;
  fileUrl?: string;
  status?: string;
  mediaType?: string;
}

interface UploadAuthorization {
  sessionId: string;
  uploadUrl: string;
  apiKey: string;
  parameters: Record<string, string | number | boolean>;
  signature: string;
}

export type UploadStage = "uploading" | "processing";
export interface UploadOptions { publicationId?: string; fileKey?: string; signal?: AbortSignal; onProgress?: (percent: number, stage: UploadStage) => void }
type UploadCursor = { publicationId?: string; auth: UploadAuthorization; offset: number; chunkId: string; done: boolean; media?: MediaDTO };
const cursors = new WeakMap<File, UploadCursor>();
const sleep = (ms: number, signal?: AbortSignal) => new Promise<void>((resolve, reject) => {
  if (signal?.aborted) { reject(new DOMException("Aborted", "AbortError")); return; }
  const abort = () => { clearTimeout(timer); reject(new DOMException("Aborted", "AbortError")); };
  const timer = setTimeout(() => { signal?.removeEventListener("abort", abort); resolve(); }, ms);
  signal?.addEventListener("abort", abort, { once: true });
});

/** Retry resumes at the last acknowledged chunk while this File remains in the composer. */
export async function uploadPostMedia(file: File, options: UploadOptions = {}): Promise<MediaDTO> {
  let cursor = cursors.get(file);
  if (cursor && cursor.publicationId !== options.publicationId) { cursors.delete(file); cursor = undefined; }
  if (cursor?.media) return cursor.media;
  if (!cursor) {
    const { data: auth } = await api.post<UploadAuthorization>("/services/mediaservice/api/media/uploads/sign", {
      fileName: file.name, mimeType: file.type, size: file.size, publicationId: options.publicationId, fileKey: options.fileKey,
    }, { signal: options.signal });
    cursor = { publicationId: options.publicationId, auth, offset: 0, chunkId: crypto.randomUUID(), done: false };
    cursors.set(file, cursor);
  }
  const chunkSize = 6 * 1024 * 1024;
  while (!cursor.done) {
    const start = cursor.offset;
    const chunked = file.type.startsWith("video/") || file.size > chunkSize;
    const end = chunked ? Math.min(start + chunkSize, file.size) : file.size;
    for (let attempt = 0; ; attempt++) {
      const form = new FormData();
      form.append("file", file.slice(start, end), file.name);
      form.append("api_key", cursor.auth.apiKey);
      form.append("signature", cursor.auth.signature);
      for (const [key, value] of Object.entries(cursor.auth.parameters)) form.append(key, String(value));
      try {
        // Separate client: the Fakebook JWT must never be sent to Cloudinary.
        await axios.post(cursor.auth.uploadUrl, form, {
          signal: options.signal,
          headers: chunked ? { "X-Unique-Upload-Id": cursor.chunkId, "Content-Range": `bytes ${start}-${end - 1}/${file.size}` } : undefined,
          onUploadProgress: event => options.onProgress?.(Math.min(95, Math.round((start + Math.min(event.loaded, end - start)) / file.size * 95)), "uploading"),
        });
        break;
      } catch (error) {
        const status = axios.isAxiosError(error) ? error.response?.status : undefined;
        if (status === 401 || status === 403) cursors.delete(file);
        if (options.signal?.aborted || attempt >= 2 || (status && status < 500 && status !== 429)) throw error;
        await sleep(1000 * 2 ** attempt, options.signal);
      }
    }
    cursor.offset = end;
    cursor.done = end === file.size;
  }
  options.onProgress?.(96, "processing");
  for (let attempt = 0; attempt < 120; attempt++) {
    try {
      const response = await api.post<MediaDTO>("/services/mediaservice/api/media/uploads/complete", { sessionId: cursor.auth.sessionId }, { signal: options.signal });
      if (!response.data.id) throw new Error("Upload did not return a media ID");
      cursor.media = response.data;
      options.onProgress?.(100, "processing");
      return response.data;
    } catch (error) {
      const status = axios.isAxiosError(error) ? error.response?.status : undefined;
      if (status === 410) { cursors.delete(file); throw new Error("Phiên tải lên đã hết hạn. Bấm thử lại để tải lại tệp."); }
      if (options.signal?.aborted || (status !== 409 && status !== 503)) throw error;
      await sleep(Math.min(2000 + attempt * 500, 10000), options.signal);
    }
  }
  throw new Error("Video vẫn đang xử lý. Bấm thử lại để kiểm tra, không cần tải lại tệp.");
}

export const uploadPostImage = uploadPostMedia;

/**
 * Upload a media file (image/video).
 */
export async function uploadMedia(file: File): Promise<MediaDTO | null> {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await api.post<MediaDTO>("/services/mediaservice/api/media/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data;
  } catch (error) {
    console.error("Failed to upload media:", error);
    return null;
  }
}

/**
 * Get resolved Media URL by Media ID.
 */
export function getMediaUrl(mediaId: string): string {
  if (!mediaId) return "";
  if (mediaId.startsWith("http://") || mediaId.startsWith("https://") || mediaId.startsWith("blob:")) {
    return mediaId;
  }
  const path = mediaId.startsWith("/") ? mediaId : `/services/mediaservice/api/media/${mediaId}/file`;
  return resolveApiUrl(path);
}
