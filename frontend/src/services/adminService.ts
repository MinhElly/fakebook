import api from "./apis";

export type AdminArea = "users" | "admins" | "posts" | "comments" | "audit";
export type AdminRow = Record<string, unknown>;
export interface AdminPage { items: AdminRow[]; total: number; page: number; size: number }
export interface AdminOverview {
  users: number; admins: number; suspended: number; pending: number; directory_ready: number;
  user_states: { active: number; suspended: number; processing: number };
  sync_states: { synced: number; pending: number; retry: number };
  days: 7 | 30; timezone: string;
  activity: { date: string; views: number; changes: number }[];
}
export const adminBase = (area: AdminArea | "overview") => `/services/${area === "posts" ? "postservice" : area === "comments" ? "commentservice" : "userservice"}/api/admin/${area}`;

export async function listAdmin(area: AdminArea, query: URLSearchParams, signal: AbortSignal): Promise<AdminPage> {
  return (await api.get(adminBase(area), { params: query, signal })).data;
}
export async function detailAdmin(area: AdminArea, id: string, signal: AbortSignal): Promise<AdminRow> {
  return (await api.get(`${adminBase(area)}/${encodeURIComponent(id)}`, { signal })).data;
}
export async function changeAdmin(area: AdminArea, id: string, action: string, reason: string, version: number, key: string) {
  return (await api.post(`${adminBase(area)}/${encodeURIComponent(id)}/${action}`, { reason, version }, { headers: { "Idempotency-Key": key } })).data;
}
