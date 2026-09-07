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

const TABS_OWN   = ["Bài viết", "Giới thiệu", "Bạn bè", "Ảnh", "Video", "Nhiều hơn"];
const TABS_OTHER = ["Dòng thời gian", "Giới thiệu", "Bạn bè", "Ảnh", "Video"];

export default function ProfileLayout({
  user, isOwn, posts, friends = [], mutualFriends = [], mutualCount = 0,
  actionButtons, onEditCover, onEditAvatar,
}: Props) {
  const navigate = useNavigate();
  const TABS = isOwn ? TABS_OWN : TABS_OTHER;
  const [activeTab, setActiveTab] = useState(TABS[0].toLowerCase());

  const coverSrc = user.cover || "/default-cover.svg";

  const bio = [
    user.bio        && { icon: "💬", text: user.bio },
    user.location   && { icon: "🏠", text: `Sống tại ${user.location}` },
    user.education  && { icon: "🎓", text: `Học tại ${user.education}` },
    user.work       && { icon: "💼", text: `Làm việc tại ${user.work}` },
    user.relationship && { icon: "❤️", text: user.relationship },
  ].filter(Boolean) as { icon: string; text: string }[];

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
            <p className="text-[#65676B] text-sm mt-0.5">
              {isOwn ? `${posts.length} bài viết · ${friends.length} bạn bè` : `${mutualCount} bạn chung`}
            </p>

            {/* Mutual friends avatars (others only) */}
            {!isOwn && mutualFriends.length > 0 && (
              <div className="flex items-center gap-1 mt-2">
                {mutualFriends.slice(0, 6).map((f, i) => (
                  <img key={f.id} src={f.avatar} alt={f.name} title={f.name}
                    className="w-8 h-8 rounded-full object-cover border-2 border-white" style={{ marginLeft: i > 0 ? -8 : 0 }} />
                ))}
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

        {/* Friends / Bạn bè tab */}
        {(activeTab === "bạn bè") && (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
            {friendList.length === 0
              ? <div className="col-span-4 bg-white rounded-xl border border-[#E4E6EB] p-10 text-center"><p className="text-4xl mb-2">👥</p><p className="text-[#65676B] font-medium">Chưa có bạn bè nào</p></div>
              : friendList.map(f => (
                  <div key={f.id} className="bg-white rounded-xl border border-[#E4E6EB] overflow-hidden hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate(`/profile/${f.id}`)}>
                    <img src={f.cover || "/default-cover.svg"} alt="" className="w-full h-20 object-cover" />
                    <div className="px-3 pb-3">
                      <div className="-mt-6 mb-1"><img src={f.avatar} alt={f.name} className="w-12 h-12 rounded-full object-cover border-2 border-white" /></div>
                      <p className="font-bold text-sm text-[#1C1E21] hover:underline leading-tight truncate">{f.name}</p>
                      {f.mutualFriends > 0 && <p className="text-xs text-[#65676B]">{f.mutualFriends} bạn chung</p>}
                    </div>
                  </div>
                ))
            }
          </div>
        )}

        {/* Posts + bio columns */}
        {(activeTab === "bài viết" || activeTab === "giới thiệu" || activeTab === "dòng thời gian") && (
          <div className="flex flex-col md:flex-row gap-4">
            {/* Left col */}
            <div className="w-full md:w-[360px] md:flex-shrink-0 space-y-3">
              {/* Bio */}
              <div className="bg-white rounded-xl shadow-sm p-4 border border-[#E4E6EB]">
                <h3 className="font-bold text-[#1C1E21] text-lg mb-3">Giới thiệu</h3>
                {bio.map(({ icon, text }) => (
                  <div key={text} className="flex items-start gap-2 py-1.5 text-sm text-[#1C1E21]">
                    <span className="text-base flex-shrink-0 mt-0.5">{icon}</span>
                    <span className="leading-snug">{text}</span>
                  </div>
                ))}
                {isOwn && onEditAvatar && (
                  <button onClick={onEditAvatar}
                    className="mt-3 w-full bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold text-sm py-2 rounded-lg transition-colors flex items-center justify-center gap-2">
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/></svg>
                    Chỉnh sửa chi tiết
                  </button>
                )}
              </div>

              {/* Photos */}
              <div className="bg-white rounded-xl shadow-sm p-4 border border-[#E4E6EB]">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-bold text-[#1C1E21] text-lg">Ảnh</h3>
                  <button className="text-[#1877F2] text-sm font-semibold hover:bg-blue-50 px-2 py-1 rounded">Xem tất cả</button>
                </div>
                <p className="py-5 text-center text-sm text-[#65676B]">Chưa có ảnh để hiển thị.</p>
              </div>

              {/* Mutual friends (others only) */}
              {!isOwn && mutualFriends.length > 0 && (
                <div className="bg-white rounded-xl shadow-sm p-4 border border-[#E4E6EB]">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="font-bold text-[#1C1E21] text-lg">Bạn chung</h3>
                    <span className="text-sm text-[#65676B]">{mutualCount} người</span>
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    {mutualFriends.slice(0, 6).map(f => (
                      <div key={f.id} className="text-center cursor-pointer group" onClick={() => navigate(`/profile/${f.id}`)}>
                        <img src={f.avatar} alt={f.name} className="w-full aspect-square rounded-xl object-cover group-hover:opacity-90 transition-opacity" />
                        <p className="text-xs font-medium text-[#1C1E21] mt-1 leading-tight truncate">{f.name}</p>
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
                    <p className="font-semibold">Chưa có bài viết nào</p>
                    {isOwn && <p className="text-sm">Hãy chia sẻ điều gì đó với bạn bè!</p>}
                  </div>
                : posts.map(post => <Post key={post.id} post={post} />)
              }
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
