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
          { icon: "⚙️", label: "Cài đặt & quyền riêng tư" },
          { icon: "❓", label: "Trợ giúp & hỗ trợ" },
          { icon: "🌙", label: "Màn hình & trợ năng" },
          { icon: "💬", label: "Đóng góp ý kiến" },
        ].map(({ icon, label }) => (
          <button key={label} className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-[#F0F2F5] transition-colors">
            <span className="w-9 h-9 bg-[#E4E6EB] rounded-full flex items-center justify-center text-lg flex-shrink-0">{icon}</span>
            <span className="font-medium text-[#1C1E21] text-sm">{label}</span>
          </button>
        ))}

        <hr className="my-2 border-[#E4E6EB]" />

        <button onClick={handleLogout} className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-[#F0F2F5] transition-colors">
          <span className="w-9 h-9 bg-[#E4E6EB] rounded-full flex items-center justify-center text-lg flex-shrink-0">🚪</span>
          <span className="font-medium text-[#1C1E21] text-sm">Đăng xuất</span>
        </button>

        <p className="text-[#65676B] text-xs px-3 mt-2">Meta © 2025</p>
      </div>
    </Dropdown>
  );
}
