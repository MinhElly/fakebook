import { useNavigate } from "react-router";
import Dropdown from "@/components/ui/Dropdown";
import { useUserStore } from "@/stores/userStore";
import { useAuth } from "@/providers/AuthProvider";

interface Props {
  onClose: () => void;
}

export default function ProfileDropdown({ onClose }: Props) {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { profile } = useUserStore();

  function handleViewProfile() {
    onClose();
    navigate("/profile");
  }

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <Dropdown className="w-[340px]">
      <div className="p-2">
        <button onClick={handleViewProfile} className="flex items-center gap-3 w-full p-2 rounded-xl hover:bg-[#F0F2F5] transition-colors">
          <img src={profile.avatar} alt="me" referrerPolicy="no-referrer" onError={(e) => { e.currentTarget.src = "/default-avatar.svg"; }} className="w-14 h-14 rounded-full object-cover flex-shrink-0" />
          <div className="text-left">
            <p className="font-bold text-[#1C1E21]">{profile.name}</p>
            <p className="text-[#1877F2] text-sm">Xem trang cá nhân của bạn</p>
          </div>
        </button>

        <hr className="my-2 border-[#E4E6EB]" />

        {[
          { icon: <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.06-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.73,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.06,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.43-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.49-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/></svg>, label: "Cài đặt & quyền riêng tư" },
          { icon: <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"/></svg>, label: "Trợ giúp & hỗ trợ" },
          { icon: <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4C12.92 3.04 12.46 3 12 3zm0 16c-3.86 0-7-3.14-7-7s3.14-7 7-7c.17 0 .34.02.51.04-1.52.88-2.51 2.56-2.51 4.46 0 2.87 2.33 5.2 5.2 5.2 1.9 0 3.58-.99 4.46-2.51.02.17.04.34.04.51 0 3.86-3.14 7-7 7z"/></svg>, label: "Màn hình & trợ năng" },
          { icon: <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zm-5.04-6.71l-2.75 3.54-1.96-2.36L6.5 17h11l-3.54-4.71z"/></svg>, label: "Đóng góp ý kiến" },
        ].map(({ icon, label }) => (
          <button key={label} className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-[#F0F2F5] transition-colors">
            <span className="w-9 h-9 bg-[#E4E6EB] rounded-full flex items-center justify-center text-[#1C1E21] flex-shrink-0">{icon}</span>
            <span className="font-medium text-[#1C1E21] text-sm">{label}</span>
          </button>
        ))}

        <hr className="my-2 border-[#E4E6EB]" />

        <button onClick={handleLogout} className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-[#F0F2F5] transition-colors">
          <span className="w-9 h-9 bg-[#E4E6EB] rounded-full flex items-center justify-center text-[#1C1E21] flex-shrink-0">
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
          </span>
          <span className="font-medium text-[#1C1E21] text-sm">Đăng xuất</span>
        </button>

        <p className="text-[#65676B] text-xs px-3 mt-2">Meta © 2025</p>
      </div>
    </Dropdown>
  );
}
