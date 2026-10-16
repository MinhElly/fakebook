import type { ReactNode } from "react"

export function Skeleton({ className = "" }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`skeleton block rounded bg-[#E4E6EB] ${className}`}
    />
  )
}

export function LoadingRegion({
  label = "Đang tải nội dung",
  children,
  className = "",
}: {
  label?: string
  children: ReactNode
  className?: string
}) {
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label={label}
      className={className}
    >
      <span className="sr-only">{label}</span>
      <div aria-hidden="true">{children}</div>
    </div>
  )
}

export function UserRowsSkeleton({
  count = 4,
  label = "Đang tải danh sách",
}: {
  count?: number
  label?: string
}) {
  return (
    <LoadingRegion label={label} className="space-y-3 p-3">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="flex items-center gap-3 py-1">
          <Skeleton className="size-10 shrink-0 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-3 w-32 max-w-full" />
            <Skeleton className="h-2 w-20" />
          </div>
        </div>
      ))}
    </LoadingRegion>
  )
}

export function PostSkeleton({ count = 2 }: { count?: number }) {
  return (
    <LoadingRegion label="Đang tải bài viết" className="space-y-4">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="rounded-xl bg-white p-4">
          <div className="mb-4 flex items-center gap-3">
            <Skeleton className="size-10 rounded-full" />
            <div className="space-y-2">
              <Skeleton className="h-3 w-36" />
              <Skeleton className="h-2 w-20" />
            </div>
          </div>
          <Skeleton className="mb-2 h-3 w-full" />
          <Skeleton className="mb-4 h-3 w-2/3" />
          <Skeleton className="aspect-[16/9] w-full" />
          <div className="mt-4 flex justify-between">
            <Skeleton className="h-4 w-16" />
            <Skeleton className="h-4 w-16" />
            <Skeleton className="h-4 w-16" />
          </div>
        </div>
      ))}
    </LoadingRegion>
  )
}

export function CommentSkeleton() {
  return (
    <LoadingRegion label="Đang tải bình luận" className="space-y-3 p-4">
      {[0, 1, 2].map((i) => (
        <div key={i} className="flex gap-2">
          <Skeleton className="size-8 shrink-0 rounded-full" />
          <div className="w-3/4 space-y-2 rounded-2xl bg-[#F0F2F5] p-3">
            <Skeleton className="h-3 w-24" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-2/3" />
          </div>
        </div>
      ))}
    </LoadingRegion>
  )
}

export function HistorySkeleton() {
  return (
    <LoadingRegion label="Đang tải lịch sử" className="space-y-4 py-4">
      {[0, 1, 2].map((i) => (
        <div key={i} className="space-y-3 border-t border-[#E4E6EB] pt-4">
          <Skeleton className="h-3 w-40" />
          <Skeleton className="h-3 w-full" />
          <Skeleton className="h-3 w-3/4" />
        </div>
      ))}
    </LoadingRegion>
  )
}

export function FriendsSkeleton() {
  return (
    <LoadingRegion label="Đang tải bạn bè">
      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
        {Array.from({ length: 6 }, (_, i) => (
          <div key={i} className="overflow-hidden rounded-lg bg-white">
            <Skeleton className="aspect-square w-full rounded-none" />
            <div className="space-y-3 p-3">
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
              <Skeleton className="h-9 w-full" />
            </div>
          </div>
        ))}
      </div>
    </LoadingRegion>
  )
}

export function ProfileSkeleton() {
  return (
    <LoadingRegion label="Đang tải hồ sơ" className="mx-auto w-full max-w-6xl">
      <Skeleton className="h-48 w-full rounded-b-xl sm:h-72" />
      <div className="flex items-end gap-4 bg-white px-4 pb-5">
        <Skeleton className="-mt-8 size-28 shrink-0 rounded-full sm:size-40" />
        <div className="mb-4 flex-1 space-y-3">
          <Skeleton className="h-6 w-48 max-w-full" />
          <Skeleton className="h-3 w-28" />
        </div>
      </div>
      <div className="mt-4 px-3">
        <PostSkeleton />
      </div>
    </LoadingRegion>
  )
}

export function AppSkeleton() {
  return (
    <main
      aria-label="Đang khởi động ứng dụng"
      className="min-h-screen bg-[#F0F2F5]"
    >
      <div className="mb-4 flex h-14 items-center gap-3 bg-white px-4">
        <Skeleton className="size-10 rounded-full" />
        <Skeleton className="h-8 w-48 max-w-[60vw] rounded-full" />
      </div>
      <div className="mx-auto max-w-2xl px-3">
        <PostSkeleton />
      </div>
    </main>
  )
}

export default Skeleton
