import { useState } from "react";
import { useNavigate } from "react-router";
import { getUserAvatarUrl, type UserSummary } from "@/services/friendsService";

export type FriendCardType = "request" | "suggestion" | "friend" | "sent_request";

interface FriendCardProps {
  user: UserSummary;
  cardType: FriendCardType;
  requestId?: string;
  friendshipId?: string;
  mutualFriendsCount?: number;
  onAccept?: (user: UserSummary, requestId?: string) => Promise<void>;
  onReject?: (user: UserSummary, requestId?: string) => Promise<void>;
  onAddFriend?: (user: UserSummary) => Promise<void>;
  onRemoveSuggestion?: (userId: string) => void;
  onCancelSent?: (user: UserSummary, requestId?: string) => Promise<void>;
  onUnfriend?: (user: UserSummary, friendshipId?: string) => Promise<void>;
}

export default function FriendCard({
  user,
  cardType,
  requestId,
  friendshipId,
  mutualFriendsCount = 0,
  onAccept,
  onReject,
  onAddFriend,
  onRemoveSuggestion,
  onCancelSent,
  onUnfriend,
}: FriendCardProps) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [actionDoneText, setActionDoneText] = useState<string | null>(null);

  // Fetch standard avatar URL (handles user.avatarUrl, user.avatarMediaId, and default avatar fallback)
  const avatarUrl = getUserAvatarUrl(user);

  const handleProfileClick = () => {
    navigate(`/profile/${user.id}`);
  };

  const handleAccept = async () => {
    if (!onAccept || loading) return;
    setLoading(true);
    try {
      await onAccept(user, requestId);
      setActionDoneText("Đã chấp nhận lời mời");
    } catch {
      setActionDoneText(null);
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    if (!onReject || loading) return;
    setLoading(true);
    try {
      await onReject(user, requestId);
      setActionDoneText("Đã xóa lời mời");
    } catch {
      setActionDoneText(null);
    } finally {
      setLoading(false);
    }
  };

  const handleAddFriend = async () => {
    if (!onAddFriend || loading) return;
    setLoading(true);
    try {
      await onAddFriend(user);
      setActionDoneText("Đã gửi lời mời");
    } catch {
      setActionDoneText(null);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelSent = async () => {
    if (!onCancelSent || loading) return;
    setLoading(true);
    try {
      await onCancelSent(user, requestId);
      setActionDoneText("Đã hủy lời mời");
    } catch {
      setActionDoneText(null);
    } finally {
      setLoading(false);
    }
  };

  const handleUnfriend = async () => {
    if (!onUnfriend || loading) return;
    if (!window.confirm(`Bạn có chắc chắn muốn hủy kết bạn với ${user.displayName || user.username}?`)) return;
    setLoading(true);
    try {
      await onUnfriend(user, friendshipId);
      setActionDoneText("Đã hủy kết bạn");
    } catch {
      setActionDoneText(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col bg-white border border-[#E4E6EB] rounded-xl overflow-hidden shadow-xs hover:shadow-md transition-shadow">
      {/* ── Avatar / Photo Top area ────────────────────────────────── */}
      <div
        onClick={handleProfileClick}
        className="relative aspect-square w-full bg-[#F0F2F5] cursor-pointer overflow-hidden group"
      >
        <img
          src={avatarUrl}
          alt={user.displayName || "Avatar"}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
          onError={(e) => {
            const target = e.target as HTMLImageElement;
            if (!target.src.includes("default-avatar.svg")) {
              target.src = "/default-avatar.svg";
            }
          }}
        />
      </div>

      {/* ── Content & Actions ──────────────────────────────────────── */}
      <div className="p-3 flex flex-col justify-between flex-1 gap-2">
        <div>
          <h3
            onClick={handleProfileClick}
            className="font-semibold text-[#050505] hover:underline cursor-pointer truncate text-base leading-tight"
            title={user.displayName || user.username}
          >
            {user.displayName || user.username || "Người dùng"}
          </h3>

          <div className="text-xs text-[#65676B] mt-1 h-4 flex items-center gap-1 truncate">
            {mutualFriendsCount > 0 ? (
              <span>{mutualFriendsCount} bạn chung</span>
            ) : user.location ? (
              <span>Sống tại {user.location}</span>
            ) : cardType === "suggestion" ? (
              <span>Gợi ý dành cho bạn</span>
            ) : cardType === "friend" ? (
              <span>Bạn bè</span>
            ) : null}
          </div>
        </div>

        {/* ── Action Buttons State Feedback ─────────────────────────── */}
        {actionDoneText ? (
          <div className="text-center py-2 px-3 bg-[#F0F2F5] text-[#65676B] text-xs font-semibold rounded-lg">
            {actionDoneText}
          </div>
        ) : (
          <div className="flex flex-col gap-1.5 mt-1">
            {cardType === "request" && (
              <>
                <button
                  disabled={loading}
                  onClick={handleAccept}
                  className="w-full py-2 bg-[#1877F2] hover:bg-[#166FE5] text-white font-semibold text-sm rounded-lg transition-colors disabled:opacity-50"
                >
                  {loading ? "Đang xử lý..." : "Xác nhận"}
                </button>
                <button
                  disabled={loading}
                  onClick={handleReject}
                  className="w-full py-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#050505] font-semibold text-sm rounded-lg transition-colors disabled:opacity-50"
                >
                  Xóa
                </button>
              </>
            )}

            {cardType === "suggestion" && (
              <>
                <button
                  disabled={loading}
                  onClick={handleAddFriend}
                  className="w-full py-2 bg-[#E7F3FF] hover:bg-[#DBEAFE] text-[#1877F2] font-semibold text-sm rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-1.5"
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clipRule="evenodd" />
                  </svg>
                  {loading ? "Đang gửi..." : "Thêm bạn bè"}
                </button>
                {onRemoveSuggestion && (
                  <button
                    disabled={loading}
                    onClick={() => onRemoveSuggestion(user.id)}
                    className="w-full py-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#050505] font-semibold text-sm rounded-lg transition-colors disabled:opacity-50"
                  >
                    Gỡ
                  </button>
                )}
              </>
            )}

            {cardType === "friend" && (
              <>
                <button
                  onClick={handleProfileClick}
                  className="w-full py-2 bg-[#E7F3FF] hover:bg-[#DBEAFE] text-[#1877F2] font-semibold text-sm rounded-lg transition-colors flex items-center justify-center gap-1.5"
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" />
                  </svg>
                  Trang cá nhân
                </button>
                <button
                  disabled={loading}
                  onClick={handleUnfriend}
                  className="w-full py-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#050505] font-semibold text-sm rounded-lg transition-colors disabled:opacity-50"
                >
                  Hủy kết bạn
                </button>
              </>
            )}

            {cardType === "sent_request" && (
              <button
                disabled={loading}
                onClick={handleCancelSent}
                className="w-full py-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#050505] font-semibold text-sm rounded-lg transition-colors disabled:opacity-50"
              >
                {loading ? "Đang hủy..." : "Hủy lời mời"}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
