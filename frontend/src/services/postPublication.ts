import api from "./apis";
export interface PublicationDraft { content: string; visibility: string; taggedUserIds: string[]; targetPostId: string | null; version: number | null }
export interface Publication { id: string; status: "PENDING" | "PUBLISHED" | "CANCELLED"; post?: { mediaIds?: string[]; [key: string]: unknown } }
const path = (id: string) => `/services/postservice/api/posts/publications/${id}`;
export async function beginPublication(id: string, draft: PublicationDraft) { return (await api.put<Publication>(path(id), draft)).data; }
export async function publicationStatus(id: string) { return (await api.get<Publication>(path(id))).data; }
export async function cancelPublication(id: string) { return (await api.delete<Publication>(path(id))).data; }
export async function publishPublication(id: string, mediaIds: string[]) {
  let publication: Publication;
  try { publication = (await api.post<Publication>(path(id) + "/publish", { mediaIds })).data; }
  catch (error) {
    // A lost response does not mean the Post transaction rolled back.
    try { publication = await publicationStatus(id); } catch { throw error; }
    if (publication.status !== "PUBLISHED" || JSON.stringify(publication.post?.mediaIds ?? []) !== JSON.stringify(mediaIds)) throw error;
  }
  if (publication.status !== "PUBLISHED" || !publication.post) throw new Error("Publication has not committed");
  return publication.post;
}
