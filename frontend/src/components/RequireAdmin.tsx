import type { ReactNode } from "react";
import { Link, Navigate } from "react-router";
import { useAuth } from "@/providers/AuthProvider";

export default function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth();
  if (isAdmin) return children;
  return <main className="grid min-h-screen place-content-center gap-4 bg-fb-bg p-6 text-fb-text">
    <h1 className="text-2xl font-bold">Bạn không có quyền quản trị</h1>
    <p>Khu vực này dành cho tài khoản Quản trị viên.</p>
    <Link className="text-fb-blue underline" to="/">Về bảng tin</Link>
  </main>;
}

export function RequireCommunity({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth();
  return isAdmin ? <Navigate to="/admin" replace /> : children;
}
