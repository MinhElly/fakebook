import api from "./apis";
import { resolveApiUrl } from "@/config/runtime-config";

export interface MediaDTO {
  id: string;
  ownerId?: string;
  url?: string;
  fileUrl?: string;
  status?: string;
  mediaType?: string;
}

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
  if (mediaId.startsWith("http://") || mediaId.startsWith("https://")) {
    return mediaId;
  }
  const path = mediaId.startsWith("/") ? mediaId : `/services/mediaservice/api/media/${mediaId}`;
  return resolveApiUrl(path);
}
