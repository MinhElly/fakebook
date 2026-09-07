import { useNavigate } from "react-router";
import { useUserStore } from "@/stores/userStore";

const NAV_LINKS = [
  { icon: "👥", label: "Bạn bè" },
  { icon: "📺", label: "Xem video" },
  { icon: "🏪", label: "Marketplace" },
  { icon: "🎮", label: "Trò chơi" },
  { icon: "📅", label: "Sự kiện" },
  { icon: "📖", label: "Kỷ niệm" },
  { icon: "🔖", label: "Đã lưu" },
  { icon: "👥", label: "Nhóm" },
];

const SHORTCUTS = [
  { name: "Hội Yêu Lập Trình",  img: "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=60&h=60&fit=crop&auto=format" },
  { name: "Du lịch Việt Nam",   img: "https://images.unsplash.com/photo-1555252333-9f8e92e65df9?w=60&h=60&fit=crop&auto=format" },
  { name: "Mua bán xe máy HN",  img: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=60&h=60&fit=crop&auto=format" },
];

export default function LeftSidebar() {
  const navigate = useNavigate();
  const { profile } = useUserStore();

  return (
    <aside
      className="
        hidden md:flex flex-col overflow-y-auto
        fixed left-0 top-14 bottom-0
        md:w-[72px] lg:w-[280px] xl:w-[360px]
        md:px-1 lg:px-2
        py-3
      "
      style={{ scrollbarWidth: "none" }}
    >
      {/* Profile shortcut */}
      <button
        onClick={() => navigate("/profile")}
        title={profile.name}
        className="flex items-center md:justify-center lg:justify-start gap-0 lg:gap-3 p-2 rounded-xl hover:bg-[#E4E6EB] transition-colors w-full mb-1"
      >
        <img src={profile.avatar} alt="me" className="w-9 h-9 rounded-full object-cover flex-shrink-0" />
        <span className="font-semibold text-[#1C1E21] hidden lg:block truncate">{profile.name}</span>
      </button>

      {/* Nav links */}
      {NAV_LINKS.map(({ icon, label }) => (
        <button
          key={label}
          title={label}
          className="flex items-center md:justify-center lg:justify-start gap-0 lg:gap-3 p-2 rounded-xl hover:bg-[#E4E6EB] transition-colors w-full"
        >
          <span className="w-9 h-9 bg-[#E4E6EB] rounded-full flex items-center justify-center text-lg flex-shrink-0">{icon}</span>
          <span className="font-medium text-[#1C1E21] hidden lg:block">{label}</span>
        </button>
      ))}

      {/* Shortcuts — only visible on lg+ */}
      <div className="hidden lg:block">
        <hr className="my-2 border-[#E4E6EB]" />
        <p className="text-[#65676B] font-semibold text-sm px-2 mb-1">Lối tắt của bạn</p>
        {SHORTCUTS.map(({ name, img }) => (
          <button
            key={name}
            className="flex items-center gap-3 p-2 rounded-xl hover:bg-[#E4E6EB] transition-colors w-full"
          >
            <img src={img} alt={name} className="w-9 h-9 rounded-lg object-cover flex-shrink-0" />
            <span className="font-medium text-[#1C1E21] text-sm truncate">{name}</span>
          </button>
        ))}
      </div>
    </aside>
  );
}
