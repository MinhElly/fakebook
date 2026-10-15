import type { ReactNode } from "react"

import { useEffect } from "react"

import { useAuth } from "@/providers/AuthProvider"

export default function RequireAuth({ children }: { children: ReactNode }) {
  const { status, login } = useAuth()

  useEffect(() => {
    if (status === "unauthenticated") void login()
  }, [status, login])

  return status === "authenticated" ? (
    children
  ) : (
    <p className="p-6" role="status">
      Đang mở trang đăng nhập…
    </p>
  )
}
