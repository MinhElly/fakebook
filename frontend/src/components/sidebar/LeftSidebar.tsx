import { useNavigate } from "react-router";
import { useUserStore } from "@/stores/userStore";

const NAV_LINKS = [
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>,
    label: "Bạn bè"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M21 3H3c-1.11 0-2 .89-2 2v12c0 1.1.89 2 2 2h5v2h8v-2h5c1.1 0 2-.9 2-2V5c0-1.11-.9-2-2-2zm0 14H3V5h18v12zM8 15l7-4-7-4v8z"/></svg>,
    label: "Xem video"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M20 4H4v2h16V4zm1 10v-2l-1-5H4l-1 5v2h1v6h10v-6h4v6h2v-6h1zm-9 4H6v-4h6v4z"/></svg>,
    label: "Marketplace"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M21.58 16.09l-1.09-7.66C20.18 6.27 18.4 5 16.25 5H7.75C5.6 5 3.82 6.27 3.51 8.43l-1.09 7.66C2.2 17.63 3.39 19 4.94 19c.68 0 1.32-.27 1.8-.75L9 16h6l2.25 2.25c.48.48 1.13.75 1.8.75 1.56 0 2.75-1.37 2.53-2.91zM11 11H9v2H8v-2H6v-1h2V8h1v2h2v1zm4-1c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm2 3c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z"/></svg>,
    label: "Trò chơi"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7v-5z"/></svg>,
    label: "Sự kiện"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>,
    label: "Kỷ niệm"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/></svg>,
    label: "Đã lưu"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>,
    label: "Nhóm"
  }
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
        <img src={profile.avatar} alt="me" referrerPolicy="no-referrer" onError={(e) => { e.currentTarget.src = "/default-avatar.svg"; }} className="w-9 h-9 rounded-full object-cover flex-shrink-0" />
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

    </aside>
  );
}
