import type { ReactNode } from "react"

import { useEffect, useRef } from "react"

import { useAuth } from "@/providers/AuthProvider"

export default function RequireAuth({ children }: { children: ReactNode }) {
  const { status, login, error } = useAuth()
  const loginStarted = useRef(false)

  useEffect(() => {
    if (status === "authenticated") loginStarted.current = false
    if (status === "unauthenticated" && !loginStarted.current) {
      loginStarted.current = true
      void login()
    }
  }, [status, login])

  if (status === "error") return (
    <div className="p-6" role="alert">
      <p>{error || "Không thể kết nối dịch vụ đăng nhập."}</p>
      <button className="mt-2 text-blue-600" onClick={() => window.location.reload()}>Thử lại</button>
    </div>
  )

  return status === "authenticated" ? (
    children
  ) : (
    <p className="p-6" role="status">
      Đang mở trang đăng nhập…
    </p>
  )
}
