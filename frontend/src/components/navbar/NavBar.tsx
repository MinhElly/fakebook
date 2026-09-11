import { useRef, useState } from "react";
import { useNavigate, useLocation } from "react-router";
import { useOutsideClick } from "@/hooks/useOutsideClick";
import MessagesDropdown from "./MessagesDropdown";
import NotificationsDropdown from "./NotificationsDropdown";
import ProfileDropdown from "./ProfileDropdown";
import { useUserStore } from "@/stores/userStore";
import type { DropdownType } from "@/types";

const NAV_ITEMS = [
  { id: "home",        path: "/",       icon: <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg> },
  { id: "friends",     path: "/friends", icon: <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg> },
  { id: "watch",       path: null,      icon: <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M21 3H3C2 3 1 4 1 5v14c0 1.1.9 2 2 2h18c1 0 2-1 2-2V5c0-1-1-2-2-2zm0 16H3V5h18v14zm-10-7l-4-3v6l4-3 4 3V9l-4 3z"/></svg> },
  { id: "marketplace", path: null,      icon: <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M3 9l1-5h16l1 5H3zm1 11v-9h16v9H4zm6-3h4v-2h-4v2z"/></svg> },
  { id: "gaming",      path: null,      icon: <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M15 7.5V2H9v5.5l3 3 3-3zM7.5 9H2v6h5.5l3-3-3-3zM9 16.5V22h6v-5.5l-3-3-3 3zM16.5 9l-3 3 3 3H22V9h-5.5z"/></svg> },
];

export default function NavBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { profile } = useUserStore();
  const [openDropdown, setOpenDropdown] = useState<DropdownType>(null);
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const [searchOpen, setSearchOpen] = useState(false);

  const msgRef = useRef<HTMLDivElement>(null);
  const notiRef = useRef<HTMLDivElement>(null);
  const profileRef = useRef<HTMLDivElement>(null);

  useOutsideClick(msgRef,     () => openDropdown === "messages"      && setOpenDropdown(null));
  useOutsideClick(notiRef,    () => openDropdown === "notifications"  && setOpenDropdown(null));
  useOutsideClick(profileRef, () => openDropdown === "profile"        && setOpenDropdown(null));

  function toggle(d: NonNullable<DropdownType>) {
    setOpenDropdown(prev => prev === d ? null : d);
  }

  const [searchQuery, setSearchQuery] = useState("");

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
      setSearchOpen(false);
    }
  }

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-white shadow-sm h-14 flex items-center px-2 md:px-4 gap-2">

      {/* ── Logo + Search ────────────────────────────────────── */}
      <div className="flex items-center gap-2 flex-shrink-0 md:w-[240px] lg:w-[280px]">
        <button onClick={() => navigate("/")} className="flex-shrink-0">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <circle cx="20" cy="20" r="20" fill="#1877F2"/>
            <path d="M27 13h-3c-1.1 0-2 .9-2 2v2h5l-.7 5H22v12h-5V22h-3v-5h3v-2c0-3.3 2.7-6 6-6h4v4z" fill="white"/>
          </svg>
        </button>

        {/* Desktop search bar */}
        <form onSubmit={handleSearchSubmit} className="hidden md:flex items-center gap-2 bg-[#F0F2F5] rounded-full px-3 h-10 flex-1">
          <svg className="w-4 h-4 text-[#65676B] flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd"/></svg>
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="bg-transparent outline-none text-sm w-full text-[#1C1E21] placeholder-[#65676B]"
            placeholder="Tìm kiếm trên Facebook"
          />
        </form>

        {/* Mobile search icon */}
        <button
          onClick={() => setSearchOpen(p => !p)}
          className="md:hidden w-10 h-10 rounded-full bg-[#E4E6EB] hover:bg-[#D8DADF] flex items-center justify-center transition-colors"
        >
          <svg className="w-5 h-5 text-[#1C1E21]" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd"/></svg>
        </button>
      </div>

      {/* Mobile search bar (expands below on mobile) */}
      {searchOpen && (
        <div className="md:hidden absolute top-14 left-0 right-0 bg-white shadow-md px-4 py-2 z-50">
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 bg-[#F0F2F5] rounded-full px-3 h-10">
            <svg className="w-4 h-4 text-[#65676B] flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd"/></svg>
            <input
              autoFocus
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-transparent outline-none text-sm w-full text-[#1C1E21] placeholder-[#65676B]"
              placeholder="Tìm kiếm trên Facebook"
            />
          </form>
        </div>
      )}

      {/* ── Center nav ───────────────────────────────────────── */}
      <div className="flex items-center gap-0.5 flex-1 justify-center">
        {NAV_ITEMS.map(({ id, path, icon }) => {
          const active = path ? location.pathname === path : activeTab === id;
          return (
            <button
              key={id}
              onClick={() => {
                if (path) { navigate(path); setActiveTab(null); }
                else setActiveTab(prev => prev === id ? null : id);
              }}
              className={`relative flex items-center justify-center h-12 rounded-lg transition-colors text-[#65676B]
                w-10 sm:w-14 md:w-16 lg:w-20 xl:w-24
                ${active ? "border-b-[3px] border-[#1877F2]" : "hover:bg-[#F0F2F5]"}`}
            >
              {icon}
              {id === "friends" && (
                <span className="absolute top-1 right-0.5 sm:right-2 bg-red-500 text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">8</span>
              )}
            </button>
          );
        })}
      </div>

      {/* ── Right action icons ───────────────────────────────── */}
      <div className="flex items-center gap-1 md:gap-2 flex-shrink-0 md:w-[240px] lg:w-[280px] justify-end">
        {/* Messages */}
        <div ref={msgRef} className="relative">
          <button
            onClick={() => toggle("messages")}
            className={`relative w-9 h-9 md:w-10 md:h-10 rounded-full flex items-center justify-center transition-colors ${openDropdown === "messages" ? "bg-[#E7F3FF] text-[#1877F2]" : "bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21]"}`}
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.2L4 17.2V4h16v12z"/></svg>
            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[16px] h-4 flex items-center justify-center px-1">3</span>
          </button>
          {openDropdown === "messages" && <MessagesDropdown />}
        </div>

        {/* Notifications */}
        <div ref={notiRef} className="relative">
          <button
            onClick={() => toggle("notifications")}
            className={`relative w-9 h-9 md:w-10 md:h-10 rounded-full flex items-center justify-center transition-colors ${openDropdown === "notifications" ? "bg-[#E7F3FF] text-[#1877F2]" : "bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21]"}`}
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[16px] h-4 flex items-center justify-center px-1">5</span>
          </button>
          {openDropdown === "notifications" && <NotificationsDropdown />}
        </div>

        {/* Profile */}
        <div ref={profileRef} className="relative">
          <button
            onClick={() => toggle("profile")}
            className={`rounded-full overflow-hidden border-2 transition-colors ${openDropdown === "profile" ? "border-[#1877F2]" : "border-transparent hover:border-[#E4E6EB]"}`}
          >
            <img
              src={profile.avatar}
              alt="profile"
              referrerPolicy="no-referrer"
              onError={(e) => { e.currentTarget.src = "/default-avatar.svg"; }}
              className="w-9 h-9 md:w-10 md:h-10 object-cover"
            />
          </button>
          {openDropdown === "profile" && <ProfileDropdown onClose={() => setOpenDropdown(null)} />}
        </div>
      </div>
    </nav>
  );
}
