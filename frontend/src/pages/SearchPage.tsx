import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router";
import {
  searchUsers,
  sendFriendRequest,
  cancelFriendRequest,
  type UserSearchResult,
  type FriendshipStatus,
} from "@/services/searchService";

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const query = searchParams.get("q") || "";

  const [activeCategory, setActiveCategory] = useState<"all" | "people">("all");
  const [results, setResults] = useState<UserSearchResult[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  // Filter toggles state (UI mock for fidelity to design)
  const [recentPostsFilter, setRecentPostsFilter] = useState(false);
  const [viewedPostsFilter, setViewedPostsFilter] = useState(false);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    searchUsers(query)
      .then((res) => {
        setResults(res.content);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [query]);

  // Handle Action Button Click (Thêm bạn bè / Hủy lời mời)
  const handleToggleFriendRequest = async (user: UserSearchResult) => {
    const currentStatus = user.friendshipStatus;
    let nextStatus: FriendshipStatus = "NONE";

    if (currentStatus === "NONE") {
      nextStatus = "PENDING_SENT";
    } else if (currentStatus === "PENDING_SENT") {
      nextStatus = "NONE";
    } else {
      return;
    }

    // Optimistic UI Update
    setResults((prev) =>
      prev.map((item) => (item.id === user.id ? { ...item, friendshipStatus: nextStatus } : item))
    );

    if (currentStatus === "NONE") {
      await sendFriendRequest(user.id);
    } else if (currentStatus === "PENDING_SENT") {
      await cancelFriendRequest(user.id);
    }
  };

  // Split results into Top Featured Match (if any is already a friend) and Others
  const featuredFriend = results.find((r) => r.friendshipStatus === "FRIEND");
  const otherPeople = results.filter((r) => r !== featuredFriend);

  return (
    <div className="flex min-h-[calc(100vh-56px)] bg-[#F0F2F5]">
      {/* ── Left Sidebar Filters ────────────────────────────────────── */}
      <aside className="hidden md:block w-80 lg:w-[360px] bg-white border-r border-[#E4E6EB] p-4 flex-shrink-0 min-h-[calc(100vh-56px)]">
        <h1 className="text-2xl font-bold text-[#1C1E21] mb-2">Kết quả tìm kiếm</h1>
        <div className="text-sm font-semibold text-[#65676B] mb-3">Bộ lọc</div>

        {/* Filter categories list */}
        <div className="space-y-1">
          {/* Tất cả */}
          <button
            onClick={() => setActiveCategory("all")}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold transition-colors ${
              activeCategory === "all" ? "bg-[#E7F3FF] text-[#1877F2]" : "hover:bg-[#F0F2F5] text-[#050505]"
            }`}
          >
            <div className={`w-9 h-9 rounded-full flex items-center justify-center ${activeCategory === "all" ? "bg-[#1877F2] text-white" : "bg-[#E4E6EB] text-[#050505]"}`}>
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" />
              </svg>
            </div>
            Tất cả
          </button>

          {/* Sub toggles under Tất cả */}
          <div className="pl-4 py-2 space-y-3 text-sm text-[#050505]">
            <div className="flex items-center justify-between">
              <span className="font-normal text-sm">Bài viết mới đây</span>
              <button
                onClick={() => setRecentPostsFilter((p) => !p)}
                className={`w-11 h-6 rounded-full transition-colors p-0.5 ${recentPostsFilter ? "bg-[#1877F2]" : "bg-[#CED0D4]"}`}
              >
                <div className={`w-5 h-5 bg-white rounded-full transition-transform ${recentPostsFilter ? "translate-x-5" : "translate-x-0"}`} />
              </button>
            </div>

            <div className="flex items-center justify-between">
              <span className="font-normal text-sm">Bài viết bạn đã xem</span>
              <button
                onClick={() => setViewedPostsFilter((p) => !p)}
                className={`w-11 h-6 rounded-full transition-colors p-0.5 ${viewedPostsFilter ? "bg-[#1877F2]" : "bg-[#CED0D4]"}`}
              >
                <div className={`w-5 h-5 bg-white rounded-full transition-transform ${viewedPostsFilter ? "translate-x-5" : "translate-x-0"}`} />
              </button>
            </div>
          </div>

          {/* Accordion Filter Items */}
          <div className="border-t border-[#E4E6EB] pt-2 space-y-1 text-sm text-[#050505]">
            {["Ngày đăng", "Bài viết của", "Vị trí được gắn thẻ"].map((filterName) => (
              <div key={filterName} className="flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#F0F2F5] cursor-pointer text-sm font-medium">
                <span>{filterName}</span>
                <svg className="w-5 h-5 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </div>
            ))}
          </div>

          {/* Category List */}
          <div className="border-t border-[#E4E6EB] pt-2 space-y-1">
            <button
              onClick={() => setActiveCategory("people")}
              className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                activeCategory === "people" ? "bg-[#E7F3FF] text-[#1877F2]" : "hover:bg-[#F0F2F5] text-[#050505]"
              }`}
            >
              <div className="w-8 h-8 rounded-full bg-[#E4E6EB] flex items-center justify-center text-[#050505]">
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
                </svg>
              </div>
              Mọi người
            </button>

            {[
              { label: "Thước phim", iconPath: "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12z" },
              { label: "Marketplace", iconPath: "M3 9l1-5h16l1 5H3zm1 11v-9h16v9H4zm6-3h4v-2h-4v2z" },
              { label: "Trang", iconPath: "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm3 11H9v-1c0-2 4-3.1 6-3.1s6 1.1 6 3.1v1z" },
              { label: "Nhóm", iconPath: "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5z" },
              { label: "Sự kiện", iconPath: "M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10z" },
            ].map(({ label, iconPath }) => (
              <div key={label} className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-[#050505] hover:bg-[#F0F2F5] cursor-pointer">
                <div className="w-8 h-8 rounded-full bg-[#E4E6EB] flex items-center justify-center text-[#050505]">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                    <path d={iconPath} />
                  </svg>
                </div>
                {label}
              </div>
            ))}
          </div>
        </div>
      </aside>

      {/* ── Main Content Area ────────────────────────────────────────── */}
      <main className="flex-1 p-4 md:p-6 max-w-4xl mx-auto">
        {!query.trim() ? (
          <div className="bg-white p-8 rounded-xl text-center shadow-sm text-[#65676B]">
            <svg className="w-16 h-16 mx-auto mb-3 text-[#B0B3B8]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <h3 className="text-lg font-bold text-[#1C1E21]">Nhập từ khóa để tìm kiếm</h3>
            <p className="text-sm mt-1">Tìm kiếm bạn bè theo tên hiển thị hoặc tên người dùng.</p>
          </div>
        ) : loading ? (
          <div className="bg-white p-8 rounded-xl text-center shadow-sm text-[#65676B]">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-[#1877F2] border-t-transparent mb-2"></div>
            <p className="text-sm font-medium">Đang tìm kiếm người dùng "{query}"...</p>
          </div>
        ) : results.length === 0 ? (
          <div className="bg-white p-8 rounded-xl text-center shadow-sm text-[#65676B]">
            <p className="text-lg font-semibold text-[#1C1E21]">Không tìm thấy kết quả nào cho "{query}"</p>
            <p className="text-sm mt-1">Hãy thử kiểm tra lại chính tả hoặc tìm kiếm bằng từ khóa khác.</p>
          </div>
        ) : (
          <div className="space-y-4">

            {/* 1. TOP FEATURED FRIEND RESULT (Match Facebook layout) */}
            {featuredFriend && (
              <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-[#E4E6EB]">
                <div className="p-4 md:p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <img
                      src={featuredFriend.avatarUrl || "/default-avatar.svg"}
                      alt={featuredFriend.displayName}
                      className="w-20 h-20 rounded-full object-cover border border-[#E4E6EB]"
                    />
                    <div>
                      <div className="flex items-center gap-2">
                        <h2
                          onClick={() => navigate(`/profile/${featuredFriend.id}`)}
                          className="text-xl font-bold text-[#050505] hover:underline cursor-pointer"
                        >
                          {featuredFriend.displayName}
                        </h2>
                      </div>
                      <p className="text-xs font-semibold text-[#65676B] mt-0.5">Bạn bè</p>

                      <div className="mt-2 space-y-1 text-xs text-[#65676B]">
                        {featuredFriend.mutualFriendsCount > 0 && (
                          <div className="flex items-center gap-1 font-semibold text-[#050505]">
                            <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20">
                              <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                            </svg>
                            <span>{featuredFriend.mutualFriendsCount} bạn chung</span>
                          </div>
                        )}
                        {featuredFriend.workplace && (
                          <div className="flex items-center gap-1.5">
                            <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M6 6V5a3 3 0 013-3h2a3 3 0 013 3v1h2a2 2 0 012 2v3.57A22.952 22.952 0 0110 13a22.95 22.95 0 01-8-1.43V8a2 2 0 012-2h2zm2-1a1 1 0 011-1h2a1 1 0 011 1v1H8V5zm1 5a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1z" clipRule="evenodd" />
                            </svg>
                            <span>{featuredFriend.workplace}</span>
                          </div>
                        )}
                        {featuredFriend.education && (
                          <div className="flex items-center gap-1.5">
                            <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20">
                              <path d="M10.394 2.08a1 1 0 00-.788 0l-7 3a1 1 0 000 1.84L5.25 8.051a.999.999 0 01.356-.257l4-1.714a1 1 0 11.788 1.838L7.667 9.088l1.94.831a1 1 0 00.787 0l7-3a1 1 0 000-1.838l-7-3zM3.31 9.397L5 10.12v4.102a8.969 8.969 0 00-2.75-1.435A1 1 0 011 11.84v-1.6a1 1 0 01.31-.734l2-1.109z" />
                            </svg>
                            <span>{featuredFriend.education}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  <button className="w-full md:w-auto px-6 py-2 bg-[#E7F3FF] hover:bg-[#DBE7F2] text-[#1877F2] font-semibold text-sm rounded-lg transition-colors flex items-center justify-center gap-2">
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z" />
                    </svg>
                    Nhắn tin
                  </button>
                </div>
              </div>
            )}

            {/* 2. OTHER PEOPLE SECTION ("Người khác") */}
            {otherPeople.length > 0 && (
              <div className="bg-white rounded-xl shadow-sm p-4 md:p-6 border border-[#E4E6EB]">
                <h3 className="text-lg font-bold text-[#1C1E21] mb-4">Người khác</h3>

                <div className="divide-y divide-[#E4E6EB]">
                  {otherPeople.map((user) => (
                    <div key={user.id} className="py-4 first:pt-0 last:pb-0 flex items-center justify-between gap-4">
                      <div className="flex items-center gap-3 min-w-0">
                        <img
                          src={user.avatarUrl || "/default-avatar.svg"}
                          alt={user.displayName}
                          className="w-14 h-14 rounded-full object-cover flex-shrink-0 border border-[#E4E6EB]"
                        />
                        <div className="min-w-0">
                          <h4
                            onClick={() => navigate(`/profile/${user.id}`)}
                            className="font-bold text-[#050505] text-base hover:underline cursor-pointer truncate"
                          >
                            {user.displayName}
                          </h4>

                          {user.bio && (
                            <p className="text-xs text-[#65676B] truncate max-w-md mt-0.5">{user.bio}</p>
                          )}

                          {user.education && !user.bio && (
                            <p className="text-xs text-[#65676B] truncate max-w-md mt-0.5">{user.education}</p>
                          )}

                          {user.mutualFriendsCount > 0 && (
                            <div className="flex items-center gap-1 text-xs text-[#65676B] mt-1">
                              <svg className="w-3.5 h-3.5 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20">
                                <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                              </svg>
                              <span>{user.mutualFriendsCount} bạn chung</span>
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Action Button */}
                      <div className="flex-shrink-0">
                        {user.friendshipStatus === "FRIEND" ? (
                          <button className="px-4 py-2 bg-[#E7F3FF] text-[#1877F2] font-semibold text-sm rounded-lg hover:bg-[#DBE7F2] transition-colors flex items-center gap-1.5">
                            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z" />
                            </svg>
                            Nhắn tin
                          </button>
                        ) : user.friendshipStatus === "PENDING_SENT" ? (
                          <button
                            onClick={() => handleToggleFriendRequest(user)}
                            className="px-4 py-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#050505] font-semibold text-sm rounded-lg transition-colors"
                          >
                            Hủy lời mời
                          </button>
                        ) : (
                          <button
                            onClick={() => handleToggleFriendRequest(user)}
                            className="px-4 py-2 bg-[#1877F2] hover:bg-[#166FE5] text-white font-semibold text-sm rounded-lg transition-colors flex items-center gap-1.5"
                          >
                            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clipRule="evenodd" />
                            </svg>
                            Thêm bạn bè
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>

                <button className="w-full mt-4 py-2 bg-[#F0F2F5] hover:bg-[#E4E6EB] text-[#050505] font-semibold text-sm rounded-lg transition-colors">
                  Xem tất cả
                </button>
              </div>
            )}

          </div>
        )}
      </main>
    </div>
  );
}
