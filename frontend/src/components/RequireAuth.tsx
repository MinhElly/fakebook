import { AppSkeleton } from "@/components/ui/Skeleton";
import { AdminShellSkeleton } from "@/pages/admin/AdminSkeleton";
import { useLocation } from "react-router";
import type { ReactNode } from "react"

import { useEffect, useRef } from "react"

import { useAuth } from "@/providers/AuthProvider"

export default function RequireAuth({ children }: { children: ReactNode }) {
  const { status, login, logout, error } = useAuth()
  const loginStarted = useRef(false)
  const { pathname } = useLocation()

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
      <button className="ml-4 mt-2 text-blue-600" onClick={() => void logout()}>Đăng xuất</button>
    </div>
  )

  return status === "authenticated" ? children : pathname === "/admin" || pathname.startsWith("/admin/") ? <AdminShellSkeleton /> : <AppSkeleton />
}
