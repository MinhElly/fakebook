import { useEffect, useRef, useState, type FormEvent } from "react";
import { useSearchParams } from "react-router";
import axios from "axios";
import api from "@/services/apis";
import { adminBase, listAdmin, detailAdmin, changeAdmin, type AdminOverview, type AdminArea, type AdminRow, type AdminPage as PageData } from "@/services/adminService";
import AdminAttachment from "./AdminAttachment";
import AdminSkeleton from "./AdminSkeleton";
import OverviewCharts from "./OverviewCharts";
import { ADMIN_TIME_NOTE, formatAdminDate as date, adminFilterDate as localDate, adminFilterInstant } from "./adminTime";

const names: Record<AdminArea | "overview", string> = { overview: "Tổng quan", users: "Người dùng", admins: "Quản trị viên", posts: "Bài viết", comments: "Bình luận", audit: "Nhật ký quản trị" };
const labels: Record<string, string> = {
  ACTIVE: "Hoạt động", SUSPENDED: "Đã khóa", UNLOCK_PENDING: "Đang mở khóa", ROLE_PENDING: "Đang đổi quyền",
  SYNCED: "Đã đồng bộ", PENDING: "Chờ đồng bộ", RETRY: "Đang thử lại", VISIBLE: "Hiển thị", HIDDEN: "Đã ẩn", DELETED: "Đã xóa",
  USER: "Người dùng", ADMIN: "Quản trị viên", true: "Có", false: "Không",
};
function text(value: unknown): string { return value == null ? "—" : labels[String(value)] || String(value); }
function failure(error: unknown) {
  if (axios.isAxiosError(error) && error.response?.status === 409) return "Dữ liệu đã thay đổi hoặc yêu cầu trước đang đồng bộ. Tải lại trước khi thao tác.";
  if (axios.isAxiosError(error) && error.response?.status === 403) return "Bạn không còn quyền thực hiện thao tác này. Vui lòng đăng nhập lại.";
  return "Không thể tải hoặc cập nhật dữ liệu. Vui lòng thử lại.";
}

export default function AdminPage({ area }: { area: AdminArea | "overview" }) {
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState<PageData | null>(null);
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [revision, setRevision] = useState(0);
  const [detail, setDetail] = useState<AdminRow | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailTarget, setDetailTarget] = useState<AdminRow | null>(null);
  const [detailError, setDetailError] = useState("");
  const [pending, setPending] = useState<{ row: AdminRow; action: string; label: string; key: string } | null>(null);
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const dialog = useRef<HTMLDialogElement>(null);
  const detailDialog = useRef<HTMLDialogElement>(null);
  const detailRequest = useRef<AbortController | null>(null);
  const query = params.toString();
  const page = Math.max(0, Number(params.get("page")) || 0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(""); setData(null); setOverview(null); setDetail(null);
    detailRequest.current?.abort(); setDetailLoading(false);
    setDetailTarget(null); setDetailError(""); detailDialog.current?.close();
    const request = area === "overview" ? api.get(adminBase("overview"), { signal: controller.signal, params: { days: new URLSearchParams(query).get("days") === "30" ? 30 : 7 } }).then(response => { if (!controller.signal.aborted) setOverview(response.data); })
      : listAdmin(area, new URLSearchParams(query), controller.signal).then(setData);
    void request.catch(cause => { if (!controller.signal.aborted) setError(failure(cause)); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => { controller.abort(); detailRequest.current?.abort(); };
  }, [area, query, revision]);

  useEffect(() => { if (pending) dialog.current?.showModal(); }, [pending]);
  useEffect(() => { if (detailTarget && !detailDialog.current?.open) detailDialog.current?.showModal(); }, [detailTarget]);
  useEffect(() => {
    if (!detailTarget && !pending) return;
    const previous = document.documentElement.style.overflow;
    document.documentElement.style.overflow = "hidden";
    return () => { document.documentElement.style.overflow = previous; };
  }, [detailTarget, pending]);
  function filter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const next = new URLSearchParams();
    new FormData(event.currentTarget).forEach((value, key) => {
      if (String(value).trim()) next.set(key, key === "from" || key === "to" ? adminFilterInstant(String(value)) : String(value).trim());
    });
    setParams(next);
  }
  async function showDetail(row: AdminRow) {
    if (area === "overview" || area === "audit" || area === "admins") return;
    detailRequest.current?.abort(); const controller = new AbortController(); detailRequest.current = controller;
    setDetailTarget(row); setDetailLoading(true); setDetail(null); setDetailError("");
    try { const result = await detailAdmin(area, String(row.id || row.subject), controller.signal); if (!controller.signal.aborted) setDetail(result); }
    catch (cause) { if (!controller.signal.aborted) setDetailError(failure(cause)); }
    finally { if (!controller.signal.aborted) setDetailLoading(false); }
  }
  function closeDetail() {
    detailRequest.current?.abort(); detailDialog.current?.close();
    setDetailTarget(null); setDetail(null); setDetailLoading(false); setDetailError("");
  }
  function closeDialog() { dialog.current?.close(); setPending(null); setReason(""); }
  async function submit(event: FormEvent) {
    event.preventDefault(); if (!pending || saving || !reason.trim() || area === "overview") return;
    setSaving(true); setError("");
    try {
      await changeAdmin(area, String(pending.row.id || pending.row.subject), pending.action, reason.trim(), Number(pending.row.version), pending.key);
      closeDialog(); setNotice("Đã ghi nhận thao tác. Các thay đổi tài khoản được đồng bộ với dịch vụ đăng nhập."); setRevision(value => value + 1);
    } catch (cause) { setError(failure(cause)); } finally { setSaving(false); }
  }
  const columns: [string, string][] = area === "audit"
    ? [["occurred_at", "Thời gian"], ["actor", "Người thực hiện"], ["target", "Đối tượng"], ["action", "Hành động"], ["reason", "Lý do"], ["before_state", "Trước"], ["after_state", "Sau"], ["request_id", "Mã yêu cầu"]]
    : area === "users" || area === "admins"
      ? [["username", "Tài khoản"], ["display_name", "Tên hiển thị"], ["email", "Email"], ["last_seen_at", "Online lần cuối (GMT+7)"], ["status", "Trạng thái"], ["sync_state", "Đồng bộ tài khoản"]]
      : [["id", "ID nội dung"], ["author_id", "Tác giả"], ["created_at", "Ngày tạo"], ["status", "Trạng thái"], ["moderation_visibility", "Kiểm duyệt"], ["community_archived", "Lưu trữ Admin"]];

  return <>
    <div className="admin-page-heading"><div><h1>{names[area]}</h1><p>{area === "admins" ? "Danh sách quản trị viên riêng, chỉ xem." : area === "posts" || area === "comments" ? "Danh sách hiển thị metadata. Mở chi tiết sẽ ghi nhật ký truy cập." : "Quản lý từ dữ liệu hiện tại của hệ thống."}</p></div>
      <button onClick={() => setRevision(value => value + 1)} disabled={loading}>Tải lại</button></div>
    <p className="admin-time-note">{ADMIN_TIME_NOTE}</p>
    {area === "overview" && <div className="admin-range" role="group" aria-label="Khoảng thời gian hoạt động quản trị"><span>Hoạt động trong</span>{([7, 30] as const).map(days => <button key={days} aria-pressed={(params.get("days") === "30" ? 30 : 7) === days} disabled={loading} onClick={() => { const next = new URLSearchParams(params); next.set("days", String(days)); setParams(next); }}>{days} ngày</button>)}</div>}
    {notice && <p className="admin-success" role="status">{notice}</p>}
    {overview?.directory_ready === 0 && <p className="admin-error" role="status">Danh mục tài khoản chưa đồng bộ đầy đủ. Ứng dụng cộng đồng đang tạm khóa cho đến khi hoàn tất backfill.</p>}
    {error && <div className="admin-error" role="alert">{error} <button onClick={() => setRevision(value => value + 1)}>Thử lại</button></div>}
    {area !== "overview" && <form key={`${area}:${query}`} className="admin-filters" onSubmit={filter}>
      <label>{area === "audit" ? "Người thực hiện" : "Tìm kiếm"}<input name={area === "audit" ? "actor" : "q"} defaultValue={params.get(area === "audit" ? "actor" : "q") || ""} placeholder={area === "users" || area === "admins" ? "Tài khoản, tên, email hoặc ID" : "Nhập ID"} /></label>
      {area === "audit" ? <>
        <label>Đối tượng<input name="target" defaultValue={params.get("target") || ""} /></label>
        <label>Hành động<input name="action" defaultValue={params.get("action") || ""} /></label>
        <label>Từ thời điểm (GMT+7)<input type="datetime-local" step="1" name="from" defaultValue={localDate(params.get("from"))} /></label>
        <label>Đến thời điểm (GMT+7)<input type="datetime-local" step="1" name="to" defaultValue={localDate(params.get("to"))} /></label>
      </> : <label>Trạng thái<select name="status" defaultValue={params.get("status") || ""}><option value="">Tất cả</option><option value="ACTIVE">Hoạt động</option>{area === "users" || area === "admins" ? <><option value="SUSPENDED">Đã khóa</option><option value="UNLOCK_PENDING">Đang mở khóa</option></> : <option value="DELETED">Đã xóa</option>}</select></label>}
      {(area === "posts" || area === "comments") && <><label>Kiểm duyệt<select name="visibility" defaultValue={params.get("visibility") || ""}><option value="">Tất cả</option><option value="VISIBLE">Hiển thị</option><option value="HIDDEN">Đã ẩn</option></select></label>
        <label>Lưu trữ Admin<select name="archived" defaultValue={params.get("archived") || ""}><option value="">Tất cả</option><option value="true">Có</option><option value="false">Không</option></select></label></>}
      <button className="admin-primary" type="submit">Áp dụng</button>
    </form>}
    {loading ? <AdminSkeleton kind={area === "overview" ? "overview" : "table"} columns={columns.length + (area === "audit" || area === "admins" ? 0 : 1)} /> : area === "overview" && overview ?
      <><dl className="admin-metrics">{([["users", "Người dùng"], ["admins", "Quản trị viên"], ["suspended", "User bị hạn chế"], ["pending", "Chờ đồng bộ"]] as const).map(([key, label]) => <div key={key}><dt>{label}</dt><dd>{overview[key].toLocaleString("vi-VN")}</dd></div>)}</dl><OverviewCharts data={overview} /></>
      : data && <section aria-label={names[area]}>
        <p className="admin-count">{data.total.toLocaleString("vi-VN")} kết quả</p>
        {data.items.length === 0 ? <div className="admin-empty"><h2>Không có kết quả phù hợp</h2><p>Thử thay đổi bộ lọc hoặc tải lại để lấy dữ liệu mới nhất.</p></div> : <div className="admin-table-wrap"><table><thead><tr>{columns.map(([key, label]) => <th key={key} scope="col">{label}</th>)}{area !== "audit" && area !== "admins" && <th scope="col">Thao tác</th>}</tr></thead>
          <tbody>{data.items.map(row => { const id = String(row.id || row.subject || row.event_id); const action = area === "users" ? (row.status === "ACTIVE" ? "suspend" : "unlock") : row.moderation_visibility === "HIDDEN" ? "restore" : "hide"; const label = { suspend: "Khóa", unlock: "Mở khóa", restore: "Khôi phục", hide: "Ẩn" }[action];
            return <tr key={id}>{columns.map(([key]) => <td key={key} className={key.includes("id") || key === "subject" || key === "actor" || key === "target" ? "admin-id" : key === "last_seen_at" ? "admin-date" : ""}>{key === "last_seen_at" && !row[key] ? "Chưa ghi nhận" : key.endsWith("_at") ? date(row[key]) : text(row[key])}</td>)}
              {area !== "audit" && area !== "admins" && <td><div className="admin-row-actions"><button onClick={() => void showDetail(row)}>Chi tiết</button><button disabled={area === "users" ? row.sync_state !== "SYNCED" : action === "restore" && (row.community_archived === true || row.status !== "ACTIVE")} onClick={() => { setError(""); setReason(""); setPending({ row, action, label, key: crypto.randomUUID() }); }}>{label}</button></div></td>}</tr>;
          })}</tbody></table></div>}
        <nav className="admin-pagination" aria-label="Phân trang"><button disabled={page === 0} onClick={() => { const next = new URLSearchParams(params); next.set("page", String(page - 1)); setParams(next); }}>Trước</button><span>Trang {page + 1}</span><button disabled={(page + 1) * data.size >= data.total} onClick={() => { const next = new URLSearchParams(params); next.set("page", String(page + 1)); setParams(next); }}>Sau</button></nav>
      </section>}
    <dialog ref={detailDialog} className="admin-dialog admin-detail-dialog" aria-labelledby="admin-detail-title" onCancel={event => { event.preventDefault(); closeDetail(); }} onClick={event => {
      if (event.target !== event.currentTarget) return;
      const rect = event.currentTarget.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeDetail();
    }}>
      <div className="admin-detail-heading"><div><h2 id="admin-detail-title">{area === "users" ? "Chi tiết người dùng" : area === "posts" ? "Chi tiết bài viết" : "Chi tiết bình luận"}</h2><p>{detailLoading ? "Đang ghi nhật ký truy cập và tải dữ liệu…" : detail ? "Lượt xem chi tiết đã được ghi nhật ký." : "Chưa tải được dữ liệu chi tiết."}</p></div><button type="button" onClick={closeDetail} autoFocus aria-label="Đóng chi tiết">Đóng</button></div>
      {detailLoading ? <AdminSkeleton kind="detail" /> : detailError ? <div className="admin-error" role="alert">{detailError} <button onClick={() => detailTarget && void showDetail(detailTarget)}>Thử lại</button></div>
        : detail && <div className="admin-detail"><dl>{Object.entries(detail).filter(([key]) => key !== "media_ids").map(([key, value]) => <div key={key}><dt>{({ content: "Nội dung", status: "Trạng thái", subject: "ID tài khoản", username: "Tài khoản", display_name: "Tên hiển thị", email: "Email", last_seen_at: "Online lần cuối (GMT+7)", account_kind: "Loại tài khoản", sync_attempts: "Số lần thử đồng bộ", author_id: "Tác giả", id: "ID", version: "Phiên bản", sync_state: "Đồng bộ tài khoản", moderation_visibility: "Kiểm duyệt", community_archived: "Lưu trữ Admin", created_at: "Ngày tạo", updated_at: "Cập nhật", post_id: "Bài viết", parent_comment_id: "Bình luận cha", effective_hidden: "Ẩn theo nhánh" } as Record<string, string>)[key] || key}</dt><dd>{key === "last_seen_at" && !value ? "Chưa ghi nhận" : key.endsWith("_at") ? date(value) : text(value)}</dd></div>)}</dl>{Array.isArray(detail.media_ids) && detail.media_ids.map((id, index) => <AdminAttachment key={String(id)} id={String(id)} index={index} />)}</div>}
    </dialog>
    <dialog ref={dialog} className="admin-dialog" onCancel={event => { if (saving) event.preventDefault(); else closeDialog(); }} aria-labelledby="admin-confirm-title">
      <form onSubmit={submit}><h2 id="admin-confirm-title">{pending?.label} {area === "users" ? "tài khoản" : "nội dung"}</h2><p>Thao tác sẽ được ghi nhật ký cùng tài khoản của bạn.</p>
        <label>Lý do<textarea required maxLength={1000} value={reason} onChange={event => setReason(event.target.value)} autoFocus disabled={saving} /></label>
        {error && <p className="admin-error" role="alert">{error}</p>}
        <div className="admin-dialog-actions"><button type="button" onClick={closeDialog} disabled={saving}>Hủy</button><button className="admin-primary" type="submit" disabled={saving || !reason.trim()}>{saving ? "Đang lưu…" : `Xác nhận ${pending?.label.toLowerCase() || ""}`}</button></div>
      </form>
    </dialog>
  </>;
}
