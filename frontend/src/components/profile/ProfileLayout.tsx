import { useState } from "react";
import { useNavigate } from "react-router";
import Post from "@/components/feed/Post";
import PostCreator from "@/components/feed/PostCreator";
import type { Post as PostType, FriendUser } from "@/types";

export interface ProfileUser {
  name: string;
  avatar: string;
  cover?: string;
  bio?: string;
  location?: string;
  education?: string;
  work?: string;
  relationship?: string;
}

interface Props {
  user: ProfileUser;
  isOwn: boolean;
  posts: PostType[];
  friends?: FriendUser[];
  mutualFriends?: FriendUser[];
  mutualCount?: number;
  actionButtons: React.ReactNode;
  onEditCover?: () => void;
  onEditAvatar?: () => void;
}

const TABS_OWN   = ["Bài viết", "Giới thiệu", "Bạn bè", "Ảnh", "Video"];
const TABS_OTHER = ["Dòng thời gian", "Giới thiệu", "Bạn bè", "Ảnh", "Video"];

export default function ProfileLayout({
  user, isOwn, posts, friends = [], mutualFriends = [], mutualCount = 0,
  actionButtons, onEditCover, onEditAvatar,
}: Props) {
  const navigate = useNavigate();
  const TABS = isOwn ? TABS_OWN : TABS_OTHER;
  const [activeTab, setActiveTab] = useState(TABS[0].toLowerCase());
  const [friendTab, setFriendTab] = useState<"all" | "mutual">("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [aboutSubTab, setAboutSubTab] = useState<"overview" | "work" | "places" | "contact">("overview");

  const coverSrc = user.cover || "/default-cover.svg";

  const bioItems = [
    user.bio          && { icon: "💬", label: "Tài khoản", text: user.bio },
    user.work         && { icon: "💼", label: "Công việc", text: `Làm việc tại ${user.work}` },
    user.education    && { icon: "🎓", label: "Học vấn", text: `Học tại ${user.education}` },
    user.location     && { icon: "🏠", label: "Nơi sống", text: `Sống tại ${user.location}` },
    user.relationship && { icon: "❤️", label: "Mối quan hệ", text: user.relationship },
  ].filter(Boolean) as { icon: string; label: string; text: string }[];

  // Collect photos from avatar, cover, and posts
  const photos = [
    user.avatar,
    user.cover,
    ...posts.map((p) => p.image).filter(Boolean),
  ].filter((src): src is string => Boolean(src) && src !== "/default-avatar.svg" && src !== "/default-cover.svg");

  // Filter friends list for Friends tab
  const displayFriendsList = (!isOwn && friendTab === "mutual") ? mutualFriends : friends;
  const filteredFriends = displayFriendsList.filter((f) =>
    f.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const friendList = isOwn ? friends : mutualFriends;

  return (
    <div className="min-h-screen bg-[#F0F2F5]">
      {/* ── Header card ────────────────────────────────────────── */}
      <div className="bg-white shadow-sm">
        <div className="max-w-[940px] mx-auto">
          {/* Cover */}
          <div className="relative">
            <img src={coverSrc} alt="cover" className="w-full h-[200px] sm:h-[280px] md:h-[350px] object-cover rounded-b-xl" />
            {isOwn && onEditCover && (
              <button onClick={onEditCover}
                className="absolute bottom-4 right-4 bg-white/90 hover:bg-white text-[#1C1E21] text-sm font-semibold px-3 py-1.5 rounded-lg flex items-center gap-2 transition-colors shadow">
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z"/></svg>
                Chỉnh sửa ảnh bìa
              </button>
            )}
          </div>

          {/* Avatar + name + actions */}
          <div className="px-4 pb-0">
            <div className="flex items-end justify-between -mt-10 sm:-mt-14 md:-mt-16 mb-3">
              {/* Avatar */}
              <div className="relative" onClick={isOwn ? onEditAvatar : undefined} style={{ cursor: isOwn ? "pointer" : "default" }}>
                <img src={user.avatar} alt={user.name}
                  className="w-[80px] h-[80px] sm:w-[120px] sm:h-[120px] md:w-[168px] md:h-[168px] rounded-full object-cover border-4 border-white hover:opacity-95 transition-opacity" />
                {isOwn && (
                  <div className="absolute bottom-1 right-1 md:bottom-3 md:right-3 bg-[#E4E6EB] hover:bg-[#D8DADF] w-7 h-7 md:w-9 md:h-9 rounded-full flex items-center justify-center shadow transition-colors">
                    <svg className="w-3.5 h-3.5 md:w-4 md:h-4 text-[#1C1E21]" fill="currentColor" viewBox="0 0 20 20"><path d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z"/></svg>
                  </div>
                )}
              </div>
              {/* Action buttons */}
              <div className="flex flex-wrap gap-2 pb-2">{actionButtons}</div>
            </div>

            {/* Name + stats */}
            <h1 className="text-xl sm:text-2xl md:text-3xl font-bold text-[#1C1E21] leading-tight">{user.name}</h1>
            {user.bio && <p className="text-[#65676B] text-sm mt-0.5">{user.bio}</p>}
            <p className="text-[#65676B] text-sm mt-0.5 font-medium">
              {isOwn
                ? `${posts.length} bài viết · ${friends.length} người bạn`
                : `${friends.length} người bạn · ${mutualCount} bạn chung`}
            </p>

            {/* Mutual friends avatars preview (others only) */}
            {!isOwn && mutualFriends.length > 0 && (
              <div className="flex items-center gap-1 mt-2">
                <div className="flex -space-x-2 overflow-hidden">
                  {mutualFriends.slice(0, 5).map((f) => (
                    <img key={f.id} src={f.avatar || "/default-avatar.svg"} alt={f.name} title={f.name}
                      className="w-8 h-8 rounded-full object-cover border-2 border-white cursor-pointer hover:opacity-90 transition-opacity"
                      onClick={() => navigate(`/profile/${f.id}`)} />
                  ))}
                </div>
                <span className="text-xs text-[#65676B] ml-2">
                  {mutualFriends.slice(0, 2).map((f) => f.name).join(", ")}
                  {mutualFriends.length > 2 ? ` và ${mutualFriends.length - 2} người bạn chung khác` : " là bạn chung"}
                </span>
              </div>
            )}

            {/* Tab bar */}
            <div className="border-t border-[#E4E6EB] mt-4 pt-0 flex gap-0 overflow-x-auto" style={{ scrollbarWidth: "none" }}>
              {TABS.map(tab => (
                <button key={tab} onClick={() => setActiveTab(tab.toLowerCase())}
                  className={`flex-shrink-0 px-3 sm:px-4 py-3 text-sm font-semibold transition-colors rounded-t-lg
                    ${activeTab === tab.toLowerCase() ? "text-[#1877F2] border-b-[3px] border-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"}`}>
                  {tab}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* ── Content ───────────────────────────────────────────── */}
      <div className="max-w-[940px] mx-auto px-4 mt-4 pb-8">

        {/* 1. Timeline / Bài viết tab */}
        {(activeTab === "bài viết" || activeTab === "dòng thời gian") && (
          <div className="flex flex-col md:flex-row gap-4">
            {/* Left col */}
            <div className="w-full md:w-[360px] md:flex-shrink-0 space-y-3">
              {/* Bio / Giới thiệu box */}
              <div className="bg-white rounded-xl shadow-sm p-4 border border-[#E4E6EB]">
                <h3 className="font-bold text-[#1C1E21] text-lg mb-3">Giới thiệu</h3>
                {bioItems.length === 0 ? (
                  <p className="text-sm text-[#65676B] italic">Chưa có thông tin giới thiệu.</p>
                ) : (
                  bioItems.map(({ icon, text }) => (
                    <div key={text} className="flex items-start gap-2.5 py-1.5 text-sm text-[#1C1E21]">
                      <span className="text-base flex-shrink-0 mt-0.5">{icon}</span>
                      <span className="leading-snug">{text}</span>
                    </div>
                  ))
                )}
                {isOwn && onEditAvatar && (
                  <button onClick={onEditAvatar}
                    className="mt-3 w-full bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold text-sm py-2 rounded-lg transition-colors flex items-center justify-center gap-2">
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/></svg>
                    Chỉnh sửa chi tiết
                  </button>
                )}
              </div>

              {/* Friends Widget Box */}
              <div className="bg-white rounded-xl shadow-sm p-4 border border-[#E4E6EB]">
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <h3 className="font-bold text-[#1C1E21] text-lg leading-tight">Bạn bè</h3>
                    <p className="text-xs text-[#65676B]">{friends.length} người bạn</p>
                  </div>
                  <button
                    onClick={() => setActiveTab("bạn bè")}
                    className="text-[#1877F2] text-sm font-semibold hover:bg-[#E7F3FF] px-2.5 py-1 rounded-lg transition-colors"
                  >
                    Xem tất cả
                  </button>
                </div>

                {friends.length === 0 ? (
                  <p className="py-4 text-center text-xs text-[#65676B]">Chưa có bạn bè nào để hiển thị.</p>
                ) : (
                  <div className="grid grid-cols-3 gap-2">
                    {friends.slice(0, 9).map((f) => (
                      <div
                        key={f.id}
                        className="text-center cursor-pointer group"
                        onClick={() => navigate(`/profile/${f.id}`)}
                      >
                        <img
                          src={f.avatar || "/default-avatar.svg"}
                          alt={f.name}
                          className="w-full aspect-square rounded-xl object-cover group-hover:opacity-90 transition-opacity bg-[#F0F2F5]"
                          onError={(e) => {
                            const target = e.target as HTMLImageElement;
                            if (!target.src.includes("default-avatar.svg")) {
                              target.src = "/default-avatar.svg";
                            }
                          }}
                        />
                        <p className="text-xs font-medium text-[#1C1E21] mt-1 leading-tight truncate group-hover:underline">
                          {f.name}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Mutual friends widget (others only) */}
              {!isOwn && mutualFriends.length > 0 && (
                <div className="bg-white rounded-xl shadow-sm p-4 border border-[#E4E6EB]">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <h3 className="font-bold text-[#1C1E21] text-lg leading-tight">Bạn chung</h3>
                      <p className="text-xs text-[#65676B]">{mutualCount} người bạn chung</p>
                    </div>
                    <button
                      onClick={() => {
                        setActiveTab("bạn bè");
                        setFriendTab("mutual");
                      }}
                      className="text-[#1877F2] text-sm font-semibold hover:bg-[#E7F3FF] px-2.5 py-1 rounded-lg transition-colors"
                    >
                      Xem tất cả
                    </button>
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    {mutualFriends.slice(0, 6).map((f) => (
                      <div key={f.id} className="text-center cursor-pointer group" onClick={() => navigate(`/profile/${f.id}`)}>
                        <img src={f.avatar || "/default-avatar.svg"} alt={f.name} className="w-full aspect-square rounded-xl object-cover group-hover:opacity-90 transition-opacity bg-[#F0F2F5]" />
                        <p className="text-xs font-medium text-[#1C1E21] mt-1 leading-tight truncate group-hover:underline">{f.name}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Right col: posts */}
            <div className="flex-1 space-y-3 min-w-0">
              {isOwn && <PostCreator />}
              {posts.length === 0
                ? <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] p-8 text-center text-[#65676B]">
                    <p className="text-4xl mb-2">📝</p>
                    <p className="font-semibold text-base text-[#1C1E21]">Chưa có bài viết nào</p>
                    {isOwn ? (
                      <p className="text-sm mt-1">Hãy chia sẻ khoảnh khắc đầu tiên của bạn với bạn bè!</p>
                    ) : (
                      <p className="text-sm mt-1">{user.name} chưa đăng bài viết nào trên trang cá nhân.</p>
                    )}
                  </div>
                : posts.map((post) => <Post key={post.id} post={post} />)
              }
            </div>
          </div>
        )}

        {/* 2. Giới thiệu / About tab */}
        {activeTab === "giới thiệu" && (
          <div className="bg-white rounded-xl border border-[#E4E6EB] p-4 sm:p-6 shadow-sm flex flex-col md:flex-row gap-6">
            {/* About Navigation Sidebar */}
            <div className="w-full md:w-[240px] flex-shrink-0 border-b md:border-b-0 md:border-r border-[#E4E6EB] pr-0 md:pr-4 pb-4 md:pb-0 space-y-1">
              <h2 className="text-xl font-bold text-[#1C1E21] mb-3 px-2">Giới thiệu</h2>
              <button
                onClick={() => setAboutSubTab("overview")}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-2.5 ${
                  aboutSubTab === "overview" ? "bg-[#E7F3FF] text-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"
                }`}
              >
                <span>📌</span> Tổng quan
              </button>
              <button
                onClick={() => setAboutSubTab("work")}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-2.5 ${
                  aboutSubTab === "work" ? "bg-[#E7F3FF] text-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"
                }`}
              >
                <span>💼</span> Công việc và học vấn
              </button>
              <button
                onClick={() => setAboutSubTab("places")}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-2.5 ${
                  aboutSubTab === "places" ? "bg-[#E7F3FF] text-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"
                }`}
              >
                <span>🏠</span> Nơi từng sống
              </button>
              <button
                onClick={() => setAboutSubTab("contact")}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-2.5 ${
                  aboutSubTab === "contact" ? "bg-[#E7F3FF] text-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"
                }`}
              >
                <span>❤️</span> Thông tin cơ bản & Mối quan hệ
              </button>
            </div>

            {/* About Content Area */}
            <div className="flex-1 space-y-6">
              {(aboutSubTab === "overview" || aboutSubTab === "work") && (
                <div>
                  <h3 className="font-bold text-[#1C1E21] text-base mb-3 border-b border-[#E4E6EB] pb-2">
                    Công việc và học vấn
                  </h3>
                  <div className="space-y-4">
                    {user.work ? (
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-full bg-[#F0F2F5] flex items-center justify-center text-lg flex-shrink-0">💼</div>
                        <div>
                          <p className="text-sm font-semibold text-[#1C1E21]">Làm việc tại {user.work}</p>
                          <p className="text-xs text-[#65676B]">Hiện tại</p>
                        </div>
                      </div>
                    ) : (
                      <p className="text-sm text-[#65676B] italic">Chưa thêm thông tin công việc.</p>
                    )}

                    {user.education ? (
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-full bg-[#F0F2F5] flex items-center justify-center text-lg flex-shrink-0">🎓</div>
                        <div>
                          <p className="text-sm font-semibold text-[#1C1E21]">Từng học tại {user.education}</p>
                          <p className="text-xs text-[#65676B]">Trường học</p>
                        </div>
                      </div>
                    ) : (
                      <p className="text-sm text-[#65676B] italic">Chưa thêm thông tin học vấn.</p>
                    )}
                  </div>
                </div>
              )}

              {(aboutSubTab === "overview" || aboutSubTab === "places") && (
                <div>
                  <h3 className="font-bold text-[#1C1E21] text-base mb-3 border-b border-[#E4E6EB] pb-2">
                    Nơi từng sống
                  </h3>
                  {user.location ? (
                    <div className="flex items-start gap-3">
                      <div className="w-10 h-10 rounded-full bg-[#F0F2F5] flex items-center justify-center text-lg flex-shrink-0">🏠</div>
                      <div>
                        <p className="text-sm font-semibold text-[#1C1E21]">Sống tại {user.location}</p>
                        <p className="text-xs text-[#65676B]">Tỉnh/Thành phố hiện tại</p>
                      </div>
                    </div>
                  ) : (
                    <p className="text-sm text-[#65676B] italic">Chưa thêm thông tin vị trí.</p>
                  )}
                </div>
              )}

              {(aboutSubTab === "overview" || aboutSubTab === "contact") && (
                <div>
                  <h3 className="font-bold text-[#1C1E21] text-base mb-3 border-b border-[#E4E6EB] pb-2">
                    Thông tin cơ bản và các mối quan hệ
                  </h3>
                  <div className="space-y-4">
                    {user.relationship ? (
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-full bg-[#F0F2F5] flex items-center justify-center text-lg flex-shrink-0">❤️</div>
                        <div>
                          <p className="text-sm font-semibold text-[#1C1E21]">{user.relationship}</p>
                          <p className="text-xs text-[#65676B]">Tình trạng quan hệ</p>
                        </div>
                      </div>
                    ) : (
                      <p className="text-sm text-[#65676B] italic">Chưa cập nhật tình trạng mối quan hệ.</p>
                    )}

                    {user.bio && (
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-full bg-[#F0F2F5] flex items-center justify-center text-lg flex-shrink-0">💬</div>
                        <div>
                          <p className="text-sm font-semibold text-[#1C1E21]">{user.bio}</p>
                          <p className="text-xs text-[#65676B]">Tiểu sử cá nhân</p>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* 3. Bạn bè / Friends tab */}
        {activeTab === "bạn bè" && (
          <div className="bg-white rounded-xl border border-[#E4E6EB] p-4 sm:p-6 shadow-sm">
            {/* Header + Search bar */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4 pb-3 border-b border-[#E4E6EB]">
              <div>
                <h2 className="text-xl font-bold text-[#1C1E21]">Bạn bè</h2>
                <p className="text-xs text-[#65676B]">
                  {isOwn ? `${friends.length} người bạn` : `${friends.length} người bạn · ${mutualCount} bạn chung`}
                </p>
              </div>

              {/* Sub-tabs for other profile */}
              {!isOwn && (
                <div className="flex bg-[#F0F2F5] p-1 rounded-lg self-start sm:self-auto">
                  <button
                    onClick={() => setFriendTab("all")}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                      friendTab === "all" ? "bg-white text-[#1877F2] shadow-xs" : "text-[#65676B] hover:text-[#1C1E21]"
                    }`}
                  >
                    Tất cả bạn bè ({friends.length})
                  </button>
                  <button
                    onClick={() => setFriendTab("mutual")}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                      friendTab === "mutual" ? "bg-white text-[#1877F2] shadow-xs" : "text-[#65676B] hover:text-[#1C1E21]"
                    }`}
                  >
                    Bạn chung ({mutualCount})
                  </button>
                </div>
              )}

              {/* Search input */}
              <div className="relative w-full sm:w-64">
                <input
                  type="text"
                  placeholder="Tìm kiếm bạn bè..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 bg-[#F0F2F5] rounded-full text-sm outline-none focus:ring-2 focus:ring-[#1877F2]"
                />
                <svg className="w-4 h-4 text-[#65676B] absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
            </div>

            {/* Grid of friends */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {filteredFriends.length === 0 ? (
                <div className="col-span-full py-12 text-center">
                  <p className="text-4xl mb-2">👥</p>
                  <p className="text-[#65676B] font-medium">
                    {searchQuery ? "Không tìm thấy bạn bè phù hợp." : "Chưa có bạn bè nào để hiển thị."}
                  </p>
                </div>
              ) : (
                filteredFriends.map((f) => (
                  <div
                    key={f.id}
                    className="bg-white rounded-xl border border-[#E4E6EB] overflow-hidden hover:shadow-md transition-shadow cursor-pointer group flex flex-col justify-between"
                    onClick={() => navigate(`/profile/${f.id}`)}
                  >
                    <div className="aspect-square bg-[#F0F2F5] overflow-hidden">
                      <img
                        src={f.avatar || "/default-avatar.svg"}
                        alt={f.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                        onError={(e) => {
                          const target = e.target as HTMLImageElement;
                          if (!target.src.includes("default-avatar.svg")) {
                            target.src = "/default-avatar.svg";
                          }
                        }}
                      />
                    </div>
                    <div className="p-3">
                      <p className="font-bold text-sm text-[#1C1E21] group-hover:underline leading-tight truncate">{f.name}</p>
                      {f.mutualFriends > 0 ? (
                        <p className="text-xs text-[#65676B] mt-0.5">{f.mutualFriends} bạn chung</p>
                      ) : (
                        <p className="text-xs text-[#65676B] mt-0.5">Bạn bè</p>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {/* 4. Ảnh / Photos tab */}
        {activeTab === "ảnh" && (
          <div className="bg-white rounded-xl border border-[#E4E6EB] p-4 sm:p-6 shadow-sm">
            <div className="mb-4 pb-3 border-b border-[#E4E6EB]">
              <h2 className="text-xl font-bold text-[#1C1E21]">Ảnh</h2>
              <p className="text-xs text-[#65676B]">Tất cả ảnh hiển thị trên trang cá nhân</p>
            </div>
            {photos.length === 0 ? (
              <div className="py-12 text-center text-[#65676B]">
                <p className="text-4xl mb-2">🖼️</p>
                <p className="font-medium">Chưa có ảnh nào để hiển thị</p>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                {photos.map((src, index) => (
                  <div key={index} className="aspect-square bg-[#F0F2F5] rounded-xl overflow-hidden group cursor-pointer border border-[#E4E6EB]">
                    <img
                      src={src}
                      alt={`Photo ${index + 1}`}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 5. Video tab */}
        {activeTab === "video" && (
          <div className="bg-white rounded-xl border border-[#E4E6EB] p-8 shadow-sm text-center text-[#65676B]">
            <p className="text-4xl mb-2">🎬</p>
            <h3 className="font-bold text-[#1C1E21] text-lg">Chưa có video nào</h3>
            <p className="text-xs mt-1">Các video được tải lên sẽ hiển thị ở đây.</p>
          </div>
        )}

      </div>
    </div>
  );
}
