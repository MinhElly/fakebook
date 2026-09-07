import { useState } from "react";
import { useNavigate } from "react-router";
import { useFriendStore } from "@/stores/friendStore";
import { PEOPLE_YOU_MAY_KNOW, FRIEND_USERS } from "@/constants/data";
import type { FriendUser } from "@/types";

type Section = "home" | "requests" | "suggestions" | "all" | "birthday" | "lists";

// ─── Icons ────────────────────────────────────────────────────────────────────
const HomeIcon    = () => <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2.1L1 12h3v9h6v-6h4v6h6v-9h3L12 2.1z"/></svg>;
const RequestIcon = () => <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>;
const SuggestIcon = () => <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>;
const AllIcon     = () => <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>;
const BdayIcon    = () => <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 6c1.11 0 2-.89 2-2 0-.36-.1-.69-.26-.99L12 0l-1.74 3.01c-.16.3-.26.63-.26.99 0 1.11.89 2 2 2zm4 3H8c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2v-8c0-1.1-.9-2-2-2z"/></svg>;
const ListIcon    = () => <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M4 6h2v2H4zm0 5h2v2H4zm0 5h2v2H4zm14-8V6H8v2h10zM8 11h10v2H8zm0 5h10v2H8z"/></svg>;
const ChevronRight = () => <svg className="w-4 h-4 text-[#606770]" fill="currentColor" viewBox="0 0 24 24"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>;
const GearIcon    = () => <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>;

// ─── Profile preview (right panel, lg+ only) ──────────────────────────────────
function ProfileCard({ user, actionBar }: { user: FriendUser; actionBar: React.ReactNode }) {
  return (
    <div className="h-full w-full overflow-y-auto bg-[#F0F2F5]" style={{ scrollbarWidth: "none" }}>
      <div className="px-4 py-4 space-y-2">
        <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] overflow-hidden">
          <img src={user.cover || "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&h=250&fit=crop&auto=format"} alt="cover" className="w-full object-cover" style={{ height: 200 }} />
          <div className="px-4 pb-4">
            <div className="flex items-end justify-between -mt-14 mb-2">
              <img src={user.avatar} alt={user.name} className="w-[112px] h-[112px] rounded-full object-cover border-4 border-white shadow-sm flex-shrink-0" />
              <div className="flex gap-2 pb-1">
                <button className="flex items-center gap-1.5 bg-[#1877F2] hover:bg-[#166FE5] text-white px-3 py-2 rounded-lg font-semibold text-sm transition-colors">
                  <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                  Phản hồi
                </button>
                <button className="flex items-center gap-1.5 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] px-3 py-2 rounded-lg font-semibold text-sm transition-colors">
                  <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>
                  Nhắn tin
                </button>
                <button className="flex items-center justify-center bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] w-9 h-9 rounded-lg transition-colors">
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
                </button>
              </div>
            </div>
            <h2 className="text-[22px] font-bold text-[#1C1E21] leading-tight">{user.name}</h2>
            <p className="text-sm text-[#65676B] mt-0.5">183 người bạn • {user.mutualFriends} bạn chung</p>
            <div className="flex flex-wrap gap-x-5 gap-y-1 mt-2">
              {user.location && <span className="flex items-center gap-1.5 text-sm text-[#65676B]"><svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path fillRule="evenodd" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" clipRule="evenodd"/></svg>{user.location}</span>}
              {user.education && <span className="flex items-center gap-1.5 text-sm text-[#65676B]"><svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path d="M12 3L1 9l4 2.18V15l7 3.82L19 15v-3.82L21 10v7h2V9L12 3z"/></svg>{user.education}</span>}
            </div>
            {user.mutualFriends > 0 && (
              <div className="flex items-center gap-2 mt-2">
                <div className="flex">{FRIEND_USERS.slice(0, 5).map((f, i) => <img key={f.id} src={f.avatar} alt={f.name} className="w-7 h-7 rounded-full object-cover border-2 border-white" style={{ marginLeft: i > 0 ? -8 : 0 }} />)}</div>
                <span className="text-sm text-[#65676B]">{user.mutualFriends} bạn chung</span>
              </div>
            )}
          </div>
        </div>
        {actionBar}
        <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] px-2">
          <div className="flex border-b border-[#E4E6EB]">
            {["Tất cả", "Giới thiệu", "Bạn bè", "Ảnh", "Check in", "Xem thêm"].map((tab, i) => (
              <button key={tab} className={`px-4 py-3 text-sm font-semibold border-b-[3px] transition-colors ${i === 0 ? "border-[#1877F2] text-[#1877F2]" : "border-transparent text-[#65676B] hover:bg-[#F0F2F5] rounded-t-lg"}`}>
                {tab}{tab === "Xem thêm" && <svg className="w-3 h-3 inline ml-1" fill="currentColor" viewBox="0 0 24 24"><path d="M7 10l5 5 5-5z"/></svg>}
              </button>
            ))}
          </div>
        </div>
        <div className="grid grid-cols-[280px_1fr] gap-3 pb-4">
          <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] p-4 self-start">
            <h3 className="font-bold text-[#1C1E21] mb-3">Thông tin cá nhân</h3>
            <div className="space-y-2.5">
              {user.location && <div className="flex items-center gap-2 text-sm text-[#1C1E21]"><svg className="w-5 h-5 text-[#65676B] flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path fillRule="evenodd" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" clipRule="evenodd"/></svg>Sống ở <strong className="ml-1">{user.location}</strong></div>}
              {user.work && <div className="flex items-center gap-2 text-sm text-[#1C1E21]"><svg className="w-5 h-5 text-[#65676B] flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path d="M20 6h-2.18c.07-.44.18-.88.18-1.36C18 2.99 16.98 2 15.64 2h-7.28C7.02 2 6 2.99 6 4.64c0 .48.11.92.18 1.36H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z"/></svg>Làm việc tại <strong className="ml-1">{user.work}</strong></div>}
              {user.education && <div className="flex items-center gap-2 text-sm text-[#1C1E21]"><svg className="w-5 h-5 text-[#65676B] flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path d="M12 3L1 9l4 2.18V15l7 3.82L19 15v-3.82L21 10v7h2V9L12 3z"/></svg>Học tại <strong className="ml-1">{user.education}</strong></div>}
              <div className="flex items-center gap-2 text-sm text-[#1C1E21]"><svg className="w-5 h-5 text-[#65676B] flex-shrink-0" fill="currentColor" viewBox="0 0 24 24"><path d="M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z"/></svg>26 tháng 12</div>
            </div>
          </div>
          <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] p-4">
            <h3 className="font-bold text-[#1C1E21] mb-3">Bài viết</h3>
            <p className="text-[#65676B] text-sm text-center py-8">Không có bài viết</p>
          </div>
        </div>
      </div>
    </div>
  );
}

function EmptyPanel({ text }: { text: string }) {
  return (
    <div className="flex-1 bg-[#F0F2F5] flex flex-col items-center justify-center gap-3">
      <svg width="110" height="88" viewBox="0 0 110 88" fill="none">
        <circle cx="44" cy="36" r="26" fill="#D8DADF"/>
        <circle cx="44" cy="26" r="11" fill="#E4E6EB"/>
        <path d="M22 58c0-12.15 9.85-22 22-22s22 9.85 22 22" fill="#D8DADF"/>
        <circle cx="76" cy="44" r="18" fill="#1877F2" opacity="0.75"/>
        <circle cx="76" cy="36" r="7" fill="#1877F2"/>
        <path d="M60 58c0-8.84 7.16-16 16-16s16 7.16 16 16" fill="#1877F2" opacity="0.75"/>
      </svg>
      <p className="text-[#65676B] text-sm font-medium max-w-[260px] text-center leading-snug">{text}</p>
    </div>
  );
}

// ─── Portrait card (grid view) ────────────────────────────────────────────────
function PortraitCard({ user, primaryLabel, primaryAction, secondaryLabel, secondaryAction }: {
  user: FriendUser; primaryLabel: string; primaryAction: () => void;
  secondaryLabel: string; secondaryAction: () => void;
}) {
  const navigate = useNavigate();
  return (
    <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] overflow-hidden hover:shadow-md transition-shadow w-[160px] sm:w-[182px] flex-shrink-0">
      <div className="overflow-hidden cursor-pointer" style={{ height: 160 }} onClick={() => navigate(`/profile/${user.id}`)}>
        <img src={user.avatar} alt={user.name} className="w-full h-full object-cover hover:scale-105 transition-transform duration-200" />
      </div>
      <div className="px-3 pt-2 pb-3">
        <p onClick={() => navigate(`/profile/${user.id}`)} className="font-bold text-[#1C1E21] text-[14px] sm:text-[15px] leading-tight hover:underline cursor-pointer mb-0.5 truncate">{user.name}</p>
        {user.mutualFriends > 0 && (
          <div className="flex items-center gap-1 mb-2">
            <div className="flex">{FRIEND_USERS.slice(0, 2).map((f, i) => <img key={f.id} src={f.avatar} alt="" className="w-4 h-4 rounded-full object-cover border border-white" style={{ marginLeft: i > 0 ? -4 : 0 }} />)}</div>
            <p className="text-[#65676B] text-xs truncate">{user.mutualFriends} bạn chung</p>
          </div>
        )}
        <button onClick={primaryAction} className="w-full bg-[#1877F2] hover:bg-[#166FE5] text-white font-semibold text-sm py-1.5 rounded-lg transition-colors mb-1.5">{primaryLabel}</button>
        <button onClick={secondaryAction} className="w-full bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold text-sm py-1.5 rounded-lg transition-colors">{secondaryLabel}</button>
      </div>
    </div>
  );
}

// ─── Request row (list view, mobile/tablet) ───────────────────────────────────
function RequestRow({ user, onAccept, onReject }: { user: FriendUser; onAccept: () => void; onReject: () => void }) {
  const navigate = useNavigate();
  return (
    <div className="flex items-center gap-3 p-3 bg-white rounded-xl border border-[#E4E6EB] mb-2">
      <img src={user.avatar} alt={user.name} onClick={() => navigate(`/profile/${user.id}`)} className="w-16 h-16 rounded-full object-cover flex-shrink-0 cursor-pointer" />
      <div className="flex-1 min-w-0">
        <p onClick={() => navigate(`/profile/${user.id}`)} className="font-semibold text-[#1C1E21] text-sm hover:underline cursor-pointer">{user.name}</p>
        {user.mutualFriends > 0 && <p className="text-xs text-[#65676B] mt-0.5">{user.mutualFriends} bạn chung</p>}
        <div className="flex gap-2 mt-2">
          <button onClick={onAccept} className="flex-1 bg-[#1877F2] hover:bg-[#166FE5] text-white font-semibold text-sm py-1.5 rounded-lg transition-colors">Xác nhận</button>
          <button onClick={onReject} className="flex-1 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold text-sm py-1.5 rounded-lg transition-colors">Xóa</button>
        </div>
      </div>
    </div>
  );
}

// ─── Main ─────────────────────────────────────────────────────────────────────
export default function FriendsPage() {
  const navigate = useNavigate();
  const [section, setSection] = useState<Section>("home");
  const [selectedUser, setSelectedUser] = useState<FriendUser | null>(null);

  const { friends, pendingReceived, acceptRequest, rejectRequest, removeFriend, sendRequest, unfollow, isFollowing, getStatus } = useFriendStore();

  const suggestions = [...PEOPLE_YOU_MAY_KNOW, ...FRIEND_USERS].filter((u, i, arr) =>
    arr.findIndex(x => x.id === u.id) === i && !friends.some(f => f.id === u.id) && getStatus(u.id) === "none"
  );

  const NAV = [
    { id: "home"        as Section, label: "Trang chủ",          icon: <HomeIcon />,    chevron: false },
    { id: "requests"    as Section, label: "Lời mời kết bạn",    icon: <RequestIcon />, chevron: true,  count: pendingReceived.length },
    { id: "suggestions" as Section, label: "Gợi ý",              icon: <SuggestIcon />, chevron: true },
    { id: "all"         as Section, label: "Tất cả bạn bè",      icon: <AllIcon />,     chevron: true,  count: friends.length },
    { id: "birthday"    as Section, label: "Sinh nhật",           icon: <BdayIcon />,   chevron: false },
    { id: "lists"       as Section, label: "Danh sách tùy chỉnh", icon: <ListIcon />,   chevron: true },
  ];

  function handleAccept(id: number) { acceptRequest(id); if (selectedUser?.id === id) setSelectedUser(null); }
  function handleReject(id: number) { rejectRequest(id); if (selectedUser?.id === id) setSelectedUser(null); }
  function handleRemove(id: number) { removeFriend(id);  if (selectedUser?.id === id) setSelectedUser(null); }
  function goSection(s: Section)    { setSection(s); setSelectedUser(null); }

  // ─── Sidebar (md: icon-only 72px, lg+: full 360px) ────────────────────────
  const sidebar = (
    <aside
      className="hidden md:flex flex-col fixed left-0 bottom-0 bg-white border-r border-[#E4E6EB] md:w-[72px] lg:w-[360px]"
      style={{ top: 56, scrollbarWidth: "none" as const }}
    >
      {/* Header */}
      <div className="flex-shrink-0 px-2 lg:px-4 pt-4 pb-2 flex items-center md:justify-center lg:justify-between">
        <div className="flex items-center gap-1">
          {section !== "home" && (
            <button onClick={() => goSection("home")} className="w-9 h-9 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors" title="Quay lại">
              <svg className="w-5 h-5 text-[#1C1E21]" fill="currentColor" viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
            </button>
          )}
          <h1 className="text-2xl font-bold text-[#1C1E21] hidden lg:block">{section === "home" ? "Bạn bè" : NAV.find(n => n.id === section)?.label}</h1>
        </div>
        <button className="w-9 h-9 rounded-full bg-[#E4E6EB] hover:bg-[#D8DADF] flex items-center justify-center transition-colors hidden lg:flex">
          <GearIcon />
        </button>
      </div>

      {/* Sub-section meta — lg+ only */}
      {section !== "home" && (
        <div className="px-4 pb-3 flex-shrink-0 hidden lg:block">
          {section === "requests" && <><p className="text-[#65676B] text-sm">{pendingReceived.length} lời mời kết bạn</p><button className="text-[#1877F2] text-sm font-semibold hover:underline">Xem lời mời đã gửi</button></>}
          {section === "all"      && <p className="text-[#65676B] text-sm">{friends.length} người bạn</p>}
        </div>
      )}

      {/* Scrollable nav/list */}
      <div className="flex-1 overflow-y-auto pb-4 md:px-1 lg:px-2" style={{ scrollbarWidth: "none" as const }}>

        {/* Home nav — icons always, labels at lg+ */}
        {section === "home" && (
          <nav className="space-y-0.5">
            {NAV.map(({ id, label, icon, chevron, count }) => (
              <button key={id} onClick={() => goSection(id)} title={label}
                className={`flex items-center md:justify-center lg:justify-start gap-0 lg:gap-3 w-full p-2 lg:px-3 lg:py-2.5 rounded-xl transition-colors
                  ${section === id ? "bg-[#E7F3FF] text-[#1877F2]" : "text-[#1C1E21] hover:bg-[#F0F2F5]"}`}
              >
                <span className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 ${section === id ? "bg-[#1877F2] text-white" : "bg-[#E4E6EB] text-[#1C1E21]"}`}>{icon}</span>
                <span className="flex-1 text-left font-semibold text-[15px] hidden lg:block">{label}</span>
                {count !== undefined && count > 0 && <span className="bg-red-500 text-white text-xs font-bold px-1.5 py-0.5 rounded-full min-w-[20px] text-center hidden lg:inline">{count}</span>}
                {chevron && !(count && count > 0) && <span className="hidden lg:inline"><ChevronRight /></span>}
              </button>
            ))}
          </nav>
        )}

        {/* Requests list — lg+ only (tablet uses main content area) */}
        {section === "requests" && (
          <div className="hidden lg:block">
            {pendingReceived.length === 0 && <p className="text-center py-8 text-[#65676B] text-sm">Không có lời mời kết bạn</p>}
            {pendingReceived.map(user => (
              <div key={user.id} onClick={() => setSelectedUser(user)}
                className={`flex items-start gap-3 p-3 rounded-xl cursor-pointer transition-colors ${selectedUser?.id === user.id ? "bg-[#E7F3FF]" : "hover:bg-[#F0F2F5]"}`}
              >
                <img src={user.avatar} alt={user.name} className="w-14 h-14 rounded-full object-cover flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <p className="font-semibold text-[#1C1E21] text-sm leading-tight">{user.name}</p>
                    <span className="text-xs text-[#65676B] flex-shrink-0">3 tuần</span>
                  </div>
                  {user.mutualFriends > 0 && <div className="flex items-center gap-1 mt-0.5"><div className="flex">{FRIEND_USERS.slice(0, 2).map((f, i) => <img key={f.id} src={f.avatar} alt="" className="w-4 h-4 rounded-full object-cover border border-white" style={{ marginLeft: i > 0 ? -3 : 0 }} />)}</div><span className="text-xs text-[#65676B]">{user.mutualFriends} bạn chung</span></div>}
                  <div className="flex gap-2 mt-2">
                    <button onClick={e => { e.stopPropagation(); handleAccept(user.id); }} className="flex-1 bg-[#1877F2] hover:bg-[#166FE5] text-white font-semibold text-xs py-1.5 rounded-lg transition-colors">Xác nhận</button>
                    <button onClick={e => { e.stopPropagation(); handleReject(user.id); }} className="flex-1 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold text-xs py-1.5 rounded-lg transition-colors">Xóa</button>
                  </div>
                </div>
              </div>
            ))}
            {/* Icon-only nav (md) for non-home sections */}
          </div>
        )}

        {/* All friends list — lg+ only */}
        {section === "all" && (
          <div className="hidden lg:block">
            {friends.length === 0 && <p className="text-center py-8 text-[#65676B] text-sm">Chưa có bạn bè nào</p>}
            {friends.map(user => (
              <div key={user.id} onClick={() => setSelectedUser(user)}
                className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer transition-colors ${selectedUser?.id === user.id ? "bg-[#E7F3FF]" : "hover:bg-[#F0F2F5]"}`}
              >
                <img src={user.avatar} alt={user.name} className="w-14 h-14 rounded-full object-cover flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-[#1C1E21] text-sm">{user.name}</p>
                  {user.mutualFriends > 0 && <p className="text-xs text-[#65676B] mt-0.5">{user.mutualFriends} bạn chung</p>}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Icon shortcuts for non-home on md (icon-only sidebar) */}
        {section !== "home" && (
          <div className="flex flex-col items-center gap-2 pt-2 md:flex lg:hidden">
            {NAV.filter(n => n.id !== "home").map(({ id, label, icon, count }) => (
              <button key={id} onClick={() => goSection(id)} title={label}
                className={`relative w-10 h-10 rounded-full flex items-center justify-center transition-colors ${section === id ? "bg-[#E7F3FF] text-[#1877F2]" : "bg-[#E4E6EB] text-[#1C1E21] hover:bg-[#D8DADF]"}`}
              >
                {icon}
                {count !== undefined && count > 0 && <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[9px] font-bold rounded-full w-4 h-4 flex items-center justify-center">{count}</span>}
              </button>
            ))}
          </div>
        )}
      </div>
    </aside>
  );

  // ─── Mobile tab strip (<md) ────────────────────────────────────────────────
  const mobileTabStrip = (
    <div className="flex md:hidden overflow-x-auto border-b border-[#E4E6EB] bg-white" style={{ scrollbarWidth: "none" as const }}>
      {NAV.map(({ id, label, icon, count }) => (
        <button key={id} onClick={() => goSection(id)}
          className={`relative flex-shrink-0 flex flex-col items-center gap-0.5 px-4 py-2.5 text-xs font-semibold transition-colors border-b-2 ${section === id ? "border-[#1877F2] text-[#1877F2]" : "border-transparent text-[#65676B]"}`}
        >
          {icon}
          <span>{label}</span>
          {count !== undefined && count > 0 && <span className="absolute top-1 right-1 bg-red-500 text-white text-[9px] font-bold rounded-full w-4 h-4 flex items-center justify-center">{count}</span>}
        </button>
      ))}
    </div>
  );

  // ─── Content margin helper ─────────────────────────────────────────────────
  // md=72px sidebar, lg=360px sidebar
  const contentCls = "ml-0 md:ml-[72px] lg:ml-[360px]";

  // ─── Home ──────────────────────────────────────────────────────────────────
  if (section === "home") return (
    <div className="min-h-screen bg-[#F0F2F5]">
      {sidebar}
      {mobileTabStrip}
      <main className={`${contentCls} px-4 sm:px-5 py-4`}>
        {pendingReceived.length > 0 && (
          <section className="mb-5">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-lg sm:text-xl font-bold text-[#1C1E21]">Lời mời kết bạn</h2>
              <button onClick={() => goSection("requests")} className="text-[#1877F2] font-semibold text-sm hover:bg-blue-50 px-2 py-1 rounded-lg">Xem tất cả</button>
            </div>
            <div className="flex flex-wrap gap-3">
              {pendingReceived.map(u => <PortraitCard key={u.id} user={u} primaryLabel="Xác nhận" primaryAction={() => handleAccept(u.id)} secondaryLabel="Xóa" secondaryAction={() => handleReject(u.id)} />)}
            </div>
          </section>
        )}
        {suggestions.length > 0 && (
          <section>
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-lg sm:text-xl font-bold text-[#1C1E21]">Những người bạn có thể biết</h2>
              <button onClick={() => goSection("suggestions")} className="text-[#1877F2] font-semibold text-sm hover:bg-blue-50 px-2 py-1 rounded-lg">Xem tất cả</button>
            </div>
            <div className="flex flex-wrap gap-3">
              {suggestions.slice(0, 10).map(u => <PortraitCard key={u.id} user={u} primaryLabel="+ Thêm bạn bè" primaryAction={() => sendRequest(u)} secondaryLabel={isFollowing(u.id) ? "Đang theo dõi" : "Xóa"} secondaryAction={() => isFollowing(u.id) ? unfollow(u.id) : {}} />)}
            </div>
          </section>
        )}
        {pendingReceived.length === 0 && suggestions.length === 0 && (
          <div className="flex flex-col items-center justify-center h-64 text-[#65676B]"><p className="text-5xl mb-3">👥</p><p className="font-medium">Không có gợi ý nào</p></div>
        )}
      </main>
    </div>
  );

  // ─── Requests ─────────────────────────────────────────────────────────────
  if (section === "requests") return (
    <div className="bg-[#F0F2F5]" style={{ height: "calc(100vh - 56px)" }}>
      {sidebar}
      {mobileTabStrip}
      {/* Mobile/tablet: full-width list */}
      <div className={`${contentCls} h-full flex overflow-hidden`}>
        {/* lg+ right panel */}
        <div className="hidden lg:flex flex-1 overflow-hidden">
          {selectedUser
            ? <ProfileCard user={selectedUser} actionBar={
                <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] px-4 py-3 flex flex-wrap items-center justify-between gap-3">
                  <p className="text-[#1C1E21] text-sm"><span className="font-semibold">{selectedUser.name.split(" ").pop()}</span> đã gửi cho bạn lời mời kết bạn</p>
                  <div className="flex gap-2">
                    <button onClick={() => handleAccept(selectedUser.id)} className="bg-[#1877F2] hover:bg-[#166FE5] text-white px-4 py-1.5 rounded-lg font-semibold text-sm transition-colors">Xác nhận lời mời</button>
                    <button onClick={() => handleReject(selectedUser.id)} className="bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] px-4 py-1.5 rounded-lg font-semibold text-sm transition-colors">Xóa lời mời</button>
                  </div>
                </div>}
              />
            : <EmptyPanel text="Chọn tên của người mà bạn muốn xem trước trang cá nhân." />
          }
        </div>
        {/* md and below: list view */}
        <div className="lg:hidden flex-1 overflow-y-auto px-4 py-4" style={{ scrollbarWidth: "none" }}>
          <h2 className="text-xl font-bold text-[#1C1E21] mb-1">Lời mời kết bạn</h2>
          <p className="text-[#65676B] text-sm mb-4">{pendingReceived.length} lời mời</p>
          {pendingReceived.length === 0 && <div className="text-center py-12 text-[#65676B]"><p className="text-4xl mb-2">👥</p><p>Không có lời mời kết bạn</p></div>}
          {pendingReceived.map(u => <RequestRow key={u.id} user={u} onAccept={() => handleAccept(u.id)} onReject={() => handleReject(u.id)} />)}
        </div>
      </div>
    </div>
  );

  // ─── All friends ───────────────────────────────────────────────────────────
  if (section === "all") return (
    <div className="bg-[#F0F2F5]" style={{ height: "calc(100vh - 56px)" }}>
      {sidebar}
      {mobileTabStrip}
      <div className={`${contentCls} h-full flex overflow-hidden`}>
        {/* lg+ split panel */}
        <div className="hidden lg:flex flex-1 overflow-hidden">
          {selectedUser
            ? <ProfileCard user={selectedUser} actionBar={
                <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] px-4 py-3 flex flex-wrap items-center justify-between gap-3">
                  <p className="text-[#1C1E21] text-sm">Bạn và <span className="font-semibold">{selectedUser.name}</span> là bạn bè</p>
                  <div className="flex gap-2">
                    <button onClick={() => navigate(`/profile/${selectedUser.id}`)} className="bg-[#1877F2] hover:bg-[#166FE5] text-white px-4 py-1.5 rounded-lg font-semibold text-sm transition-colors">Xem trang cá nhân</button>
                    <button onClick={() => handleRemove(selectedUser.id)} className="bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] px-4 py-1.5 rounded-lg font-semibold text-sm transition-colors">Hủy kết bạn</button>
                  </div>
                </div>}
              />
            : <EmptyPanel text="Chọn tên của người bạn muốn xem thông tin." />
          }
        </div>
        {/* md and below: grid cards */}
        <div className="lg:hidden flex-1 overflow-y-auto px-4 py-4" style={{ scrollbarWidth: "none" }}>
          <h2 className="text-xl font-bold text-[#1C1E21] mb-4">Tất cả bạn bè ({friends.length})</h2>
          {friends.length === 0 && <div className="text-center py-12 text-[#65676B]"><p className="text-4xl mb-2">👥</p><p>Chưa có bạn bè nào</p></div>}
          <div className="flex flex-wrap gap-3">
            {friends.map(u => (
              <div key={u.id} className="bg-white rounded-xl border border-[#E4E6EB] overflow-hidden hover:shadow-md transition-shadow w-[160px] sm:w-[182px] cursor-pointer" onClick={() => navigate(`/profile/${u.id}`)}>
                <div className="w-full overflow-hidden" style={{ height: 160 }}><img src={u.avatar} alt={u.name} className="w-full h-full object-cover hover:scale-105 transition-transform" /></div>
                <div className="px-3 py-2"><p className="font-bold text-[#1C1E21] text-sm hover:underline truncate">{u.name}</p>{u.mutualFriends > 0 && <p className="text-xs text-[#65676B]">{u.mutualFriends} bạn chung</p>}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );

  // ─── Suggestions ───────────────────────────────────────────────────────────
  if (section === "suggestions") return (
    <div className="min-h-screen bg-[#F0F2F5]">
      {sidebar}
      {mobileTabStrip}
      <main className={`${contentCls} px-4 sm:px-5 py-4`}>
        <h2 className="text-lg sm:text-xl font-bold text-[#1C1E21] mb-4">Những người bạn có thể biết</h2>
        <div className="flex flex-wrap gap-3">
          {suggestions.map(u => <PortraitCard key={u.id} user={u} primaryLabel="+ Thêm bạn bè" primaryAction={() => sendRequest(u)} secondaryLabel={isFollowing(u.id) ? "Đang theo dõi" : "Xóa"} secondaryAction={() => isFollowing(u.id) ? unfollow(u.id) : {}} />)}
        </div>
      </main>
    </div>
  );

  // ─── Fallback ─────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-[#F0F2F5]">
      {sidebar}
      {mobileTabStrip}
      <div className={`${contentCls} flex flex-col items-center justify-center h-[60vh]`}>
        <p className="text-5xl mb-3">🚧</p>
        <p className="text-[#65676B] font-medium">Tính năng đang được phát triển</p>
      </div>
    </div>
  );
}
