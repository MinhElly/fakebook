import { useEffect, useRef, useState } from "react";
import api from "@/services/apis";

export default function AdminAttachment({ id, index }: { id: string; index: number }) {
  const [asset, setAsset] = useState<{ url: string; type: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const request = useRef<AbortController | null>(null);
  useEffect(() => () => request.current?.abort(), []);
  useEffect(() => () => { if (asset) URL.revokeObjectURL(asset.url); }, [asset]);
  async function load() {
    const controller = new AbortController(); request.current = controller; setLoading(true); setError("");
    try {
      const response = await api.get(`/services/mediaservice/api/admin/media/${encodeURIComponent(id)}/file`, { responseType: "blob", signal: controller.signal });
      if (!controller.signal.aborted) setAsset({ url: URL.createObjectURL(response.data), type: response.data.type });
    } catch { if (!controller.signal.aborted) setError("Không thể tải tệp. Vui lòng thử lại."); }
    finally { if (!controller.signal.aborted) setLoading(false); }
  }
  return <div className="admin-attachment">
    {asset ? asset.type.startsWith("image/") ? <img src={asset.url} alt={`Tệp đính kèm ${index + 1}`} /> : asset.type.startsWith("video/") ? <video src={asset.url} controls aria-label={`Tệp đính kèm ${index + 1}`} /> : <a href={asset.url} download={`tep-${index + 1}`}>Tải tệp đính kèm {index + 1}</a>
      : <button onClick={() => void load()} disabled={loading}>{loading ? "Đang ghi nhật ký và tải tệp…" : `Xem tệp đính kèm ${index + 1}`}</button>}
    {error && <p role="alert">{error}</p>}
  </div>;
}
