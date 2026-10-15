import { useEffect, useRef, useState } from "react"

import api from "@/services/apis"

interface Revision {
  revision: number
  editedAt: string
  changedFields: string[]
  snapshot: {
    content: string
    visibility: string
    mediaIds?: string[]
    taggedUserIds?: string[]
  }
}

const changeLabels: Record<string, string> = {
  BASELINE: "Bản ghi ban đầu",
  content: "Nội dung",
  media: "Ảnh đính kèm",
  tags: "Người được gắn thẻ",
  visibility: "Quyền riêng tư",
};
const privacyLabels: Record<string, string> = {
  PUBLIC: "Công khai", FRIENDS: "Bạn bè", PRIVATE: "Chỉ mình tôi",
};

function collectionChange(current: string[] = [], previous?: string[]) {
  if (!previous) return `${current.length}`;
  const added = current.filter(id => !previous.includes(id)).length;
  const removed = previous.filter(id => !current.includes(id)).length;
  return `${current.length} (thêm ${added}, gỡ ${removed})`;
}

export default function PostHistory({
  id,
  onClose,
}: {
  id: string
  onClose: () => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)

  const [rows, setRows] = useState<Revision[]>([])

  const [loading, setLoading] = useState(false)

  const [error, setError] = useState(false)

  const [more, setMore] = useState(true)

  async function load(before?: number) {
    setLoading(true)
    setError(false)

    try {
      const { data } = await api.get<Revision[]>(
        `/services/postservice/api/posts/${id}/history`,
        { params: { before, size: 20 } },
      )

      setRows((previous) =>
        before === undefined ? data : [...previous, ...data],
      )
      setMore(data.length === 20)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    dialog.current?.showModal()
    void load()
  }, [id])

  return (
    <dialog
      ref={dialog}
      onCancel={onClose}
      onClose={onClose}
      className="m-auto max-h-[85vh] w-[min(560px,95vw)] rounded-xl p-5 backdrop:bg-black/50"
    >
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">Lịch sử chỉnh sửa</h2>
        <button
          autoFocus
          onClick={onClose}
          aria-label="Đóng lịch sử"
          className="rounded p-2"
        >
          ✕
        </button>
      </div>
      <p className="my-3 text-sm text-gray-600">
        Ảnh cũ không được lưu trong lịch sử.
      </p>
      {rows.map((row, index) => (
        <article key={row.revision} className="border-t py-4">
          <p className="text-sm text-gray-600">
            {new Date(row.editedAt).toLocaleString()} · Phiên bản {row.revision}
          </p>
          <p className="my-2 whitespace-pre-wrap">{row.snapshot.content}</p>
          <p className="text-sm">
            Thay đổi: {row.changedFields.map(field => changeLabels[field] || field).join(", ")}
          </p>
          {row.changedFields.includes("media") && <p className="text-sm">Ảnh: {collectionChange(row.snapshot.mediaIds, rows[index + 1]?.snapshot.mediaIds)}</p>}
          {row.changedFields.includes("tags") && <p className="text-sm">Người được gắn thẻ: {collectionChange(row.snapshot.taggedUserIds, rows[index + 1]?.snapshot.taggedUserIds)}</p>}
          <p className="text-sm">
            Quyền riêng tư: {row.changedFields.includes("visibility") && rows[index + 1] && <>{privacyLabels[rows[index + 1].snapshot.visibility]} → </>}
            {privacyLabels[row.snapshot.visibility] || row.snapshot.visibility}
          </p>
        </article>
      ))}
      {!loading && !error && rows.length === 0 && (
        <p>Bài viết chưa có lịch sử chỉnh sửa được ghi nhận.</p>
      )}
      {loading && <p role="status">Đang tải lịch sử…</p>}
      {error && (
        <p role="alert">
          Không tải được lịch sử.{" "}
          <button onClick={() => void load(rows.at(-1)?.revision)}>
            Thử lại
          </button>
        </p>
      )}
      {more && !loading && !error && rows.length > 0 && (
        <button
          className="p-2 text-blue-600"
          onClick={() => void load(rows.at(-1)?.revision)}
        >
          Xem thêm
        </button>
      )}
    </dialog>
  )
}
