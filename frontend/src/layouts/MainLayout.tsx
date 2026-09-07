import { Outlet, Navigate } from "react-router";
import NavBar from "@/components/navbar/NavBar";
import { useAuth } from "@/providers/AuthProvider";

export default function MainLayout() {
  const { isLoggedIn } = useAuth();

  if (!isLoggedIn) return <Navigate to="/login" replace />;

  return (
    <div className="min-h-screen bg-[#F0F2F5]" style={{ fontFamily: "'Inter', system-ui, sans-serif" }}>
      <NavBar />
      <div className="pt-14">
        <Outlet />
      </div>
    </div>
  );
}
