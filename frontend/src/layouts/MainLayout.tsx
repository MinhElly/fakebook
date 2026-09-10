import { Outlet, Navigate } from "react-router";
import NavBar from "@/components/navbar/NavBar";
import { useAuth } from "@/providers/AuthProvider";
import { useFriendRequestRealtime } from "@/hooks/useFriendRequestRealtime";
import FriendRequestToast from "@/components/ui/FriendRequestToast";

export default function MainLayout() {
  const { status } = useAuth();
  const {
    activeToastRequest,
    handleAcceptToast,
    handleRejectToast,
    handleCloseToast,
  } = useFriendRequestRealtime(status === "authenticated", 4000); // Polling 4s background

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

      {/* Real-time Friend Request Toast Notification at Bottom-Left */}
      {activeToastRequest && (
        <FriendRequestToast
          request={activeToastRequest}
          onAccept={handleAcceptToast}
          onReject={handleRejectToast}
          onClose={handleCloseToast}
        />
      )}
    </div>
  );
}
