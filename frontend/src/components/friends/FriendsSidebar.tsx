import type { FriendTabType } from "@/services/friendsService";

interface FriendsSidebarProps {
  activeTab: FriendTabType;
  onTabChange: (tab: FriendTabType) => void;
  pendingRequestsCount: number;
  onOpenSentModal: () => void;
}

export default function FriendsSidebar({
  activeTab,
  onTabChange,
  pendingRequestsCount,
  onOpenSentModal,
}: FriendsSidebarProps) {
  const menuItems = [
    {
      id: "overview" as FriendTabType,
      label: "Trang chủ bạn bè",
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
          <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" />
        </svg>
      ),
    },
    {
      id: "requests" as FriendTabType,
      label: "Lời mời kết bạn",
      badge: pendingRequestsCount > 0 ? pendingRequestsCount : null,
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
          <path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9 0c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4zm9 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V20h6v-2c0-2.66-5.33-4-7-4z" />
        </svg>
      ),
    },
    {
      id: "suggestions" as FriendTabType,
      label: "Gợi ý",
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
          <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
        </svg>
      ),
    },
    {
      id: "all" as FriendTabType,
      label: "Tất cả bạn bè",
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
          <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
        </svg>
      ),
    },
    {
      id: "following" as FriendTabType,
      label: "Đang theo dõi",
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
          <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
        </svg>
      ),
    },
    {
      id: "followers" as FriendTabType,
      label: "Người theo dõi",
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" />
        </svg>
      ),
    },
  ];

  return (
    <>
      {/* ── Desktop Left Sidebar ───────────────────────────────────── */}
      <aside className="hidden md:block w-80 lg:w-[360px] bg-white border-r border-[#E4E6EB] p-4 flex-shrink-0 min-h-[calc(100vh-56px)] sticky top-14">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-2xl font-bold text-[#1C1E21]">Bạn bè</h1>
          <button
            onClick={onOpenSentModal}
            title="Xem lời mời đã gửi"
            className="p-2 rounded-full hover:bg-[#F0F2F5] text-[#65676B] hover:text-[#1877F2] transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </div>

        <nav className="space-y-1">
          {menuItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-semibold transition-colors ${
                  isActive
                    ? "bg-[#E7F3FF] text-[#1877F2]"
                    : "hover:bg-[#F0F2F5] text-[#050505]"
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-9 h-9 rounded-full flex items-center justify-center ${
                      isActive
                        ? "bg-[#1877F2] text-white"
                        : "bg-[#E4E6EB] text-[#050505]"
                    }`}
                  >
                    {item.icon}
                  </div>
                  <span>{item.label}</span>
                </div>

                {item.badge !== null && item.badge !== undefined && (
                  <span className="bg-[#E41E3F] text-white text-xs font-bold px-2 py-0.5 rounded-full">
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        <hr className="my-4 border-[#E4E6EB]" />

        <div className="px-3">
          <button
            onClick={onOpenSentModal}
            className="w-full flex items-center justify-between text-left text-sm font-medium text-[#1877F2] hover:underline py-1"
          >
            <span>Lời mời đã gửi</span>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </aside>

      {/* ── Mobile Horizontal Pill Navigation ────────────────────── */}
      <div className="md:hidden bg-white border-b border-[#E4E6EB] p-2 overflow-x-auto flex items-center gap-2 scrollbar-none sticky top-14 z-30">
        {menuItems.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className={`flex-shrink-0 flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap ${
                isActive
                  ? "bg-[#1877F2] text-white"
                  : "bg-[#E4E6EB] text-[#050505]"
              }`}
            >
              <span>{item.label}</span>
              {item.badge !== null && item.badge !== undefined && (
                <span className="bg-[#E41E3F] text-white text-[10px] px-1.5 py-0.2 rounded-full">
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
        <button
          onClick={onOpenSentModal}
          className="flex-shrink-0 bg-[#F0F2F5] text-[#1877F2] px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap"
        >
          Lời mời đã gửi
        </button>
      </div>
    </>
  );
}
