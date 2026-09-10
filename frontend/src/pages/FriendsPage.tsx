import { useState, useEffect, useMemo, useCallback } from "react";
import { useSearchParams } from "react-router";
import FriendsSidebar from "@/components/friends/FriendsSidebar";
import FriendCard from "@/components/friends/FriendCard";
import SentRequestsModal from "@/components/friends/SentRequestsModal";
import Toast from "@/components/ui/Toast";
import {
  getCurrentUserProfile,
  getFriendRequests,
  getSentFriendRequests,
  getFriendSuggestions,
  getAllFriends,
  acceptFriendRequest,
  rejectFriendRequest,
  sendFriendRequest,
  cancelFriendRequest,
  unfriend,
  type FriendRequestItem,
  type FriendshipItem,
  type FriendSuggestionItem,
  type FriendTabType,
  type UserSummary,
} from "@/services/friendsService";

export default function FriendsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get("tab") as FriendTabType) || "overview";

  const [myUser, setMyUser] = useState<UserSummary | null>(null);
  const [requests, setRequests] = useState<FriendRequestItem[]>([]);
  const [sentRequests, setSentRequests] = useState<FriendRequestItem[]>([]);
  const [suggestions, setSuggestions] = useState<FriendSuggestionItem[]>([]);
  const [friends, setFriends] = useState<FriendshipItem[]>([]);

  const [loading, setLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [isSentModalOpen, setIsSentModalOpen] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const handleTabChange = (tab: FriendTabType) => {
    setSearchParams({ tab });
  };

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  // Load all initial data from REST API
  const loadData = useCallback(async () => {
    setLoading(true);
    setErrorMsg(null);

    try {
      // 1. Get current logged-in user profile
      const user = await getCurrentUserProfile();
      setMyUser(user);

      if (user && user.id) {
        // 2. Parallel fetch requests, sent requests, suggestions & friends concurrently
        const [reqRes, sentRes, sugRes, friendsRes] = await Promise.all([
          getFriendRequests(user.id).catch(() => []),
          getSentFriendRequests(user.id).catch(() => []),
          getFriendSuggestions(user.id).catch(() => []),
          getAllFriends(user.id).catch(() => []),
        ]);

        setRequests(reqRes);
        setSentRequests(sentRes);
        setSuggestions(sugRes);
        setFriends(friendsRes);
      }
    } catch (err) {
      console.error("Lỗi khi tải dữ liệu trang bạn bè:", err);
      setErrorMsg("Không thể kết nối với máy chủ API. Vui lòng kiểm tra lại dịch vụ Backend.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handlers for friend actions
  const handleAcceptRequest = async (user: UserSummary, requestId?: string) => {
    if (!requestId) return;
    const req = requests.find((r) => r.id === requestId);
    if (!req) return;

    await acceptFriendRequest(req);
    setRequests((prev) => prev.filter((r) => r.id !== requestId));
    showToast(`Đã trở thành bạn bè với ${user.displayName || user.username}!`);
    // Refresh friend list
    if (myUser) {
      getAllFriends(myUser.id).then(setFriends).catch(console.error);
    }
  };

  const handleRejectRequest = async (user: UserSummary, requestId?: string) => {
    if (!requestId) return;
    await rejectFriendRequest(requestId);
    setRequests((prev) => prev.filter((r) => r.id !== requestId));
    showToast(`Đã xóa lời mời của ${user.displayName || user.username}.`);
  };

  const handleAddFriend = async (user: UserSummary) => {
    if (!myUser) return;
    const newReq = await sendFriendRequest(myUser.id, user.id);
    setSuggestions((prev) => prev.filter((s) => s.id !== user.id));
    setSentRequests((prev) => [newReq, ...prev]);
    showToast(`Đã gửi lời mời kết bạn tới ${user.displayName || user.username}.`);
  };

  const handleRemoveSuggestion = (userId: string) => {
    setSuggestions((prev) => prev.filter((s) => s.id !== userId));
  };

  const handleCancelSentRequest = async (user: UserSummary, requestId?: string) => {
    if (!requestId) return;
    await cancelFriendRequest(requestId);
    setSentRequests((prev) => prev.filter((r) => r.id !== requestId));
    showToast(`Đã hủy lời mời kết bạn với ${user.displayName || user.username}.`);
  };

  const handleUnfriend = async (user: UserSummary, friendshipId?: string) => {
    if (!user || !user.id) return;
    await unfriend(user.id);
    setFriends((prev) =>
      prev.filter((f) => f.friend?.id !== user.id && f.user?.id !== user.id && f.id !== friendshipId)
    );
    showToast(`Đã hủy kết bạn với ${user.displayName || user.username}.`);
  };

  // Filtered friends list for tab 'all'
  const filteredFriends = useMemo(() => {
    if (!searchQuery.trim()) return friends;
    const q = searchQuery.toLowerCase().trim();
    return friends.filter((f) => {
      const friendUser = f.friend?.id === myUser?.id ? f.user : f.friend;
      const name = friendUser?.displayName || friendUser?.username || "";
      return name.toLowerCase().includes(q);
    });
  }, [friends, searchQuery, myUser]);

  return (
    <div className="flex flex-col md:flex-row min-h-[calc(100vh-56px)] bg-[#F0F2F5]">
      {/* ── Left Sidebar Navigation ────────────────────────────────── */}
      <FriendsSidebar
        activeTab={activeTab}
        onTabChange={handleTabChange}
        pendingRequestsCount={requests.length}
        onOpenSentModal={() => setIsSentModalOpen(true)}
      />

      {/* ── Right Content Area ────────────────────────────────────── */}
      <main className="flex-1 p-4 md:p-6 lg:p-8 max-w-[1400px] w-full mx-auto">
        {errorMsg && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center justify-between">
            <span>{errorMsg}</span>
            <button onClick={loadData} className="font-semibold underline hover:no-underline ml-4">
              Thử lại
            </button>
          </div>
        )}

        {/* ── TAB 1: OVERVIEW ───────────────────────────────────────── */}
        {activeTab === "overview" && (
          <div className="space-y-8">
            {/* Lời mời kết bạn section */}
            <section>
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-xl font-bold text-[#1C1E21]">Lời mời kết bạn</h2>
                  <p className="text-sm text-[#65676B]">
                    {requests.length > 0
                      ? `Bạn có ${requests.length} lời mời kết bạn chưa phản hồi`
                      : "Không có lời mời kết bạn mới"}
                  </p>
                </div>
                {requests.length > 0 && (
                  <button
                    onClick={() => handleTabChange("requests")}
                    className="text-sm font-semibold text-[#1877F2] hover:underline"
                  >
                    Xem tất cả ({requests.length})
                  </button>
                )}
              </div>

              {loading ? (
                <SkeletonGrid />
              ) : requests.length === 0 ? (
                <div className="bg-white rounded-xl border border-[#E4E6EB] p-8 text-center">
                  <div className="w-12 h-12 bg-[#F0F2F5] rounded-full flex items-center justify-center mx-auto mb-2 text-[#65676B]">
                    <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9 0c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4zm9 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V20h6v-2c0-2.66-5.33-4-7-4z" />
                    </svg>
                  </div>
                  <p className="text-sm font-medium text-[#65676B]">Không có lời mời kết bạn mới nào.</p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5">
                  {requests.slice(0, 5).map((req) => (
                    <FriendCard
                      key={req.id}
                      user={req.sender}
                      requestId={req.id}
                      cardType="request"
                      onAccept={handleAcceptRequest}
                      onReject={handleRejectRequest}
                    />
                  ))}
                </div>
              )}
            </section>

            <hr className="border-[#E4E6EB]" />

            {/* Gợi ý kết bạn section */}
            <section>
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-xl font-bold text-[#1C1E21]">Những người bạn có thể biết</h2>
                  <p className="text-sm text-[#65676B]">Gợi ý dựa trên thông tin cá nhân và bạn chung</p>
                </div>
                {suggestions.length > 0 && (
                  <button
                    onClick={() => handleTabChange("suggestions")}
                    className="text-sm font-semibold text-[#1877F2] hover:underline"
                  >
                    Xem tất cả
                  </button>
                )}
              </div>

              {loading ? (
                <SkeletonGrid />
              ) : suggestions.length === 0 ? (
                <div className="bg-white rounded-xl border border-[#E4E6EB] p-8 text-center">
                  <p className="text-sm font-medium text-[#65676B]">Hiện chưa có thêm gợi ý kết bạn mới nào.</p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5">
                  {suggestions.slice(0, 10).map((user) => (
                    <FriendCard
                      key={user.id}
                      user={user}
                      cardType="suggestion"
                      mutualFriendsCount={user.mutualFriendsCount}
                      onAddFriend={handleAddFriend}
                      onRemoveSuggestion={handleRemoveSuggestion}
                    />
                  ))}
                </div>
              )}
            </section>
          </div>
        )}

        {/* ── TAB 2: REQUESTS ───────────────────────────────────────── */}
        {activeTab === "requests" && (
          <div>
            <div className="flex items-center justify-between mb-6">
              <div>
                <h1 className="text-2xl font-bold text-[#1C1E21]">Lời mời kết bạn</h1>
                <p className="text-sm text-[#65676B] mt-0.5">
                  {requests.length > 0 ? `Tất cả ${requests.length} lời mời chờ phản hồi` : "Danh sách trống"}
                </p>
              </div>
              <button
                onClick={() => setIsSentModalOpen(true)}
                className="text-sm font-semibold text-[#1877F2] bg-[#E7F3FF] hover:bg-[#DBEAFE] px-4 py-2 rounded-lg transition-colors"
              >
                Xem lời mời đã gửi
              </button>
            </div>

            {loading ? (
              <SkeletonGrid />
            ) : requests.length === 0 ? (
              <div className="bg-white rounded-2xl border border-[#E4E6EB] p-12 text-center max-w-md mx-auto my-8">
                <div className="w-16 h-16 bg-[#F0F2F5] rounded-full flex items-center justify-center mx-auto mb-3 text-[#65676B]">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9 0c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4zm9 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V20h6v-2c0-2.66-5.33-4-7-4z" />
                  </svg>
                </div>
                <h3 className="text-lg font-bold text-[#050505]">Không có lời mời kết bạn nào</h3>
                <p className="text-sm text-[#65676B] mt-1">
                  Khi người khác gửi lời mời kết bạn cho bạn, lời mời sẽ xuất hiện ở đây.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5">
                {requests.map((req) => (
                  <FriendCard
                    key={req.id}
                    user={req.sender}
                    requestId={req.id}
                    cardType="request"
                    onAccept={handleAcceptRequest}
                    onReject={handleRejectRequest}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* ── TAB 3: SUGGESTIONS ────────────────────────────────────── */}
        {activeTab === "suggestions" && (
          <div>
            <div className="mb-6">
              <h1 className="text-2xl font-bold text-[#1C1E21]">Gợi ý bạn bè</h1>
              <p className="text-sm text-[#65676B] mt-0.5">Khám phá và kết nối với những người bạn có thể biết</p>
            </div>

            {loading ? (
              <SkeletonGrid />
            ) : suggestions.length === 0 ? (
              <div className="bg-white rounded-2xl border border-[#E4E6EB] p-12 text-center max-w-md mx-auto my-8">
                <p className="text-base font-semibold text-[#050505]">Hiện chưa có thêm gợi ý kết bạn nào</p>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5">
                {suggestions.map((user) => (
                  <FriendCard
                    key={user.id}
                    user={user}
                    cardType="suggestion"
                    mutualFriendsCount={user.mutualFriendsCount}
                    onAddFriend={handleAddFriend}
                    onRemoveSuggestion={handleRemoveSuggestion}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* ── TAB 4: ALL FRIENDS ────────────────────────────────────── */}
        {activeTab === "all" && (
          <div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
              <div>
                <h1 className="text-2xl font-bold text-[#1C1E21]">Tất cả bạn bè</h1>
                <p className="text-sm text-[#65676B] mt-0.5">
                  {friends.length} người bạn trong danh sách của bạn
                </p>
              </div>

              {/* Real-time search filter */}
              <div className="relative w-full sm:w-72">
                <svg
                  className="w-4 h-4 text-[#65676B] absolute left-3 top-1/2 -translate-y-1/2"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                    clipRule="evenodd"
                  />
                </svg>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Tìm kiếm bạn bè..."
                  className="w-full pl-9 pr-4 py-2 bg-white border border-[#CED0D4] rounded-full text-sm outline-none focus:border-[#1877F2] text-[#050505]"
                />
              </div>
            </div>

            {loading ? (
              <SkeletonGrid />
            ) : filteredFriends.length === 0 ? (
              <div className="bg-white rounded-2xl border border-[#E4E6EB] p-12 text-center max-w-md mx-auto my-8">
                <p className="text-base font-semibold text-[#050505]">
                  {searchQuery ? "Không tìm thấy bạn bè phù hợp" : "Bạn chưa có người bạn nào trong danh sách"}
                </p>
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery("")}
                    className="mt-2 text-sm text-[#1877F2] font-semibold hover:underline"
                  >
                    Xóa từ khóa tìm kiếm
                  </button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5">
                {filteredFriends.map((f) => {
                  const friendUser = f.friend?.id === myUser?.id ? f.user : f.friend;
                  return (
                    <FriendCard
                      key={f.id}
                      user={friendUser}
                      friendshipId={f.id}
                      cardType="friend"
                      onUnfriend={handleUnfriend}
                    />
                  );
                })}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Sent Requests Modal */}
      <SentRequestsModal
        isOpen={isSentModalOpen}
        onClose={() => setIsSentModalOpen(false)}
        sentRequests={sentRequests}
        onCancelRequest={async (req) => {
          await handleCancelSentRequest(req.receiver, req.id);
        }}
        loading={loading}
      />

      {/* Toast Notification */}
      {toastMessage && <Toast message={toastMessage} type="success" onClose={() => setToastMessage(null)} />}
    </div>
  );
}

// Skeleton placeholder while data is loading from backend
function SkeletonGrid() {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5 animate-pulse">
      {Array.from({ length: 10 }).map((_, i) => (
        <div key={i} className="bg-white border border-[#E4E6EB] rounded-xl overflow-hidden">
          <div className="aspect-square bg-[#E4E6EB] w-full" />
          <div className="p-3 space-y-2">
            <div className="h-4 bg-[#E4E6EB] rounded-md w-3/4" />
            <div className="h-3 bg-[#E4E6EB] rounded-md w-1/2" />
            <div className="h-8 bg-[#E4E6EB] rounded-lg w-full mt-2" />
          </div>
        </div>
      ))}
    </div>
  );
}
