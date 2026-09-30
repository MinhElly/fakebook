import { useNavigate } from "react-router";
import { useUserStore } from "@/stores/userStore";

const NAV_LINKS = [
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>,
    label: "Bạn bè",
    path: "/friends"
  },
  {
    icon: <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/></svg>,
    label: "Đã lưu",
    path: "/saved"
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
      {NAV_LINKS.map(({ icon, label, path }) => (
        <button
          key={label}
          title={label}
          onClick={() => navigate(path)}
          className="flex items-center md:justify-center lg:justify-start gap-0 lg:gap-3 p-2 rounded-xl hover:bg-[#E4E6EB] transition-colors w-full"
        >
          <span className="w-9 h-9 bg-[#E4E6EB] rounded-full flex items-center justify-center text-lg flex-shrink-0">{icon}</span>
          <span className="font-medium text-[#1C1E21] hidden lg:block">{label}</span>
        </button>
      ))}

    </aside>
  );
}
