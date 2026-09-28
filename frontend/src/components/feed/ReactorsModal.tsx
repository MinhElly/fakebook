import { useEffect, useState } from "react"
import api from "@/services/apis"
import { fetchPostReactors, type PostReactor } from "@/services/reactionService"

interface Props {
  postId: string
  onClose: () => void
}

interface ReactorView extends PostReactor {
  name: string
  avatar: string
}

const reactionLabels: Record<PostReactor["reactionType"], string> = {
  LIKE: "Thích",
  LOVE: "Yêu thích",
  HAHA: "Haha",
  WOW: "Wow",
  SAD: "Buồn",
  ANGRY: "Phẫn nộ",
}

export default function ReactorsModal({ postId, onClose }: Props) {
  const [reactors, setReactors] = useState<ReactorView[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function loadReactors() {
      try {
        const reactionRows = await fetchPostReactors(postId)
        const userIds = [...new Set(reactionRows.map((row) => row.userId))]
        const profileMap = new Map<string, {
          name: string
          avatarMediaId?: string
        }>()

        if (userIds.length > 0) {
          const profileResponse = await api.get(
            "/services/userservice/api/user-profiles/public",
            {
              params: { "id.in": userIds.join(",") },
            },
          )

          for (const profile of profileResponse.data ?? []) {
            profileMap.set(profile.id, {
              name: profile.displayName || profile.username || "Người dùng",
              avatarMediaId: profile.avatarMediaId,
            })
          }
        }

        const avatarIds = [
          ...new Set(
            [...profileMap.values()]
              .map((profile) => profile.avatarMediaId)
              .filter(Boolean),
          ),
        ] as string[]
        const mediaMap = new Map<string, string>()
        if (avatarIds.length > 0) {
          const mediaResponse = await api.get(
            "/services/mediaservice/api/media",
            {
              params: { "id.in": avatarIds.join(",") },
            },
          )
          for (const media of mediaResponse.data ?? [])
            mediaMap.set(media.id, media.url)
        }

        if (!cancelled) {
          setReactors(
            reactionRows.map((row) => {
              const profile = profileMap.get(row.userId)
              return {
                ...row,
                name: profile?.name ?? "Người dùng",
                avatar: profile?.avatarMediaId
                  ? (mediaMap.get(profile.avatarMediaId) ??
                    "/default-avatar.svg")
                  : "/default-avatar.svg",
              }
            }),
          )
        }
      } catch (error) {
        console.error("Không tải được danh sách người đã react", error)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void loadReactors()
    return () => {
      cancelled = true
    }
  }, [postId])

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/40 p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-xl bg-white shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-[#E4E6EB] px-4 py-3">
          <h3 className="text-lg font-bold text-[#1C1E21]">Cảm xúc</h3>
          <button
            className="h-9 w-9 rounded-full bg-[#E4E6EB] text-xl text-[#65676B]"
            onClick={onClose}
          >
            ×
          </button>
        </div>
        <div className="max-h-[60vh] overflow-y-auto p-2">
          {loading && (
            <p className="p-4 text-center text-[#65676B]">Đang tải...</p>
          )}
          {!loading && reactors.length === 0 && (
            <p className="p-4 text-center text-[#65676B]">
              Chưa có cảm xúc nào.
            </p>
          )}
          {reactors.map((reactor) => (
            <div
              key={reactor.userId}
              className="flex items-center gap-3 rounded-lg px-3 py-2 hover:bg-[#F0F2F5]"
            >
              <img
                src={reactor.avatar}
                alt={reactor.name}
                className="h-10 w-10 rounded-full object-cover"
              />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-[#1C1E21]">
                  {reactor.name}
                </p>
                <p className="text-xs text-[#65676B]">
                  {reactionLabels[reactor.reactionType]}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
