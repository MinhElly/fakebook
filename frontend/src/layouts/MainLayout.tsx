import { Outlet, Navigate } from "react-router";
import NavBar from "@/components/navbar/NavBar";
import { useAuth } from "@/providers/AuthProvider";
import { useFriendRequestRealtime } from "@/hooks/useFriendRequestRealtime";
import FriendRequestToast from "@/components/ui/FriendRequestToast";
import SplashScreen from "@/components/ui/SplashScreen";

export default function MainLayout() {
  const { status } = useAuth();
  const {
    activeToastRequest,
    handleAcceptToast,
    handleRejectToast,
    handleCloseToast,
  } = useFriendRequestRealtime(status === "authenticated", 4000); // Polling 4s background

  if (status === "initializing") {
    return <SplashScreen />;
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
