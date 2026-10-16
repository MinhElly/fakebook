import { useEffect, useRef, useState } from "react"

import {
  getCommentHistory,
  type CommentRevision,
} from "@/services/commentService"

import { HistorySkeleton } from "@/components/ui/Skeleton"

import LoadError from "@/components/ui/LoadError"

export default function CommentHistory({
  id,

  onClose,
}: {
  id: string

  onClose: () => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)

  const generation = useRef(0)

  const pending = useRef(false)

  const [rows, setRows] = useState<CommentRevision[]>([])

  const [loading, setLoading] = useState(true)

  const [error, setError] = useState(false)

  const [more, setMore] = useState(false)

  async function load(before?: number) {
    if (pending.current) return

    pending.current = true

    const request = generation.current

    setLoading(true)

    setError(false)

    try {
      const data = await getCommentHistory(id, before)

      if (request !== generation.current) return

      setRows((prev) => (before === undefined ? data : [...prev, ...data]))

      setMore(data.length === 20)
    } catch {
      if (request === generation.current) setError(true)
    } finally {
      if (request === generation.current) {
        pending.current = false

        setLoading(false)
      }
    }
  }

  useEffect(() => {
    dialog.current?.showModal()

    generation.current++

    pending.current = false

    setRows([])

    setMore(false)

    void load()

    return () => {
      generation.current++

      pending.current = false
    }
  }, [id])

  return (
    <dialog
      ref={dialog}
      onCancel={onClose}
      onClose={onClose}
      aria-labelledby="comment-history-title"
      className="m-auto max-h-[85dvh] w-[min(560px,95vw)] overflow-hidden rounded-xl bg-white p-0 text-[#1C1E21] backdrop:bg-black/50 open:flex open:flex-col"
    >
      <header className="flex shrink-0 items-center justify-between gap-3 bg-white px-5 py-4">
        <h2 id="comment-history-title" className="text-xl font-bold">
          Lịch sử chỉnh sửa bình luận
        </h2>
        <button
          autoFocus
          onClick={onClose}
          aria-label="Đóng lịch sử"
          className="rounded-full p-2 hover:bg-[#F0F2F5]"
        >
          ✕
        </button>
      </header>
      <div className="min-h-0 overflow-y-auto overscroll-contain px-5 pb-5">
        <div aria-busy={loading}>
          {rows.map((row) => (
            <article
              key={row.revision}
              className="mt-3 border-t border-[#E4E6EB] py-3"
            >
              <p className="text-sm text-[#65676B]">
                {row.revision === 0
                  ? "Bản gốc được ghi nhận"
                  : `Lần chỉnh sửa ${row.revision}`}{" "}
                ·{" "}
                {row.editedAt
                  ? new Date(row.editedAt).toLocaleString("vi-VN")
                  : "Không xác định thời gian"}
              </p>
              <p className="mt-2 whitespace-pre-wrap break-words text-sm">
                {row.content}
              </p>
            </article>
          ))}
        </div>
        {loading && <HistorySkeleton />}
        {error && (
          <LoadError
            message="Không tải được lịch sử bình luận."
            onRetry={() => void load(rows.at(-1)?.revision)}
          />
        )}
        {!loading && !error && rows.length === 0 && (
          <p className="py-5 text-sm text-[#65676B]">
            Bình luận chưa có lịch sử chỉnh sửa được ghi nhận.
          </p>
        )}
        {more && !loading && !error && (
          <button
            onClick={() => void load(rows.at(-1)?.revision)}
            className="py-2 font-semibold text-[#1877F2]"
          >
            Xem thêm
          </button>
        )}
      </div>
    </dialog>
  )
}
