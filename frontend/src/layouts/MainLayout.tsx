import { Outlet, Navigate } from "react-router";
import NavBar from "@/components/navbar/NavBar";
import { useAuth } from "@/providers/AuthProvider";

export default function MainLayout() {
  const { status } = useAuth();

  if (status === "initializing") {
    return <div className="flex min-h-screen items-center justify-center bg-[#F0F2F5] text-sm font-medium text-[#65676B]" role="status">Đang kiểm tra phiên đăng nhập...</div>;
  }

  if (status !== "authenticated") return <Navigate to="/login" replace />;

  return (
    <div className="min-h-screen bg-[#F0F2F5]" style={{ fontFamily: "'Inter', system-ui, sans-serif" }}>
      <NavBar />
      <div className="pt-14">
        <Outlet />
      </div>
    </div>
  );
}
