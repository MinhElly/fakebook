import { UserRowsSkeleton } from "@/components/ui/Skeleton";
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router";
import { Emoji, EmojiStyle } from "emoji-picker-react";
import MediaImage from "@/components/feed/MediaImage";
import { useAuth } from "@/providers/AuthProvider";
import api from "@/services/apis";
import { getUserAvatarUrl, sendFriendRequest, type UserSummary, type FriendshipItem, type FriendRequestItem } from "@/services/friendsService";
import { fetchPostReactors, type PostReactor, type ReactionType } from "@/services/reactionService";

interface Props { postId: string; onClose: () => void }
interface ReactorView extends PostReactor { name: string; avatar: string }
type Relationship = "friend" | "sent" | "received";
const types: ReactionType[] = ["LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY"];
const labels: Record<ReactionType, string> = { LIKE: "Thích", LOVE: "Yêu thích", HAHA: "Haha", WOW: "Wow", SAD: "Buồn", ANGRY: "Phẫn nộ" };
const emoji: Partial<Record<ReactionType, string>> = { HAHA: "1f606", WOW: "1f62e", SAD: "1f622", ANGRY: "1f621" };

function ReactionIcon({ type, size = 20 }: { type: ReactionType; size?: number }) {
  return <span aria-hidden="true" style={{ width: size, height: size }} className={`inline-flex shrink-0 items-center justify-center rounded-full ${type === "LIKE" ? "bg-[#1877F2]" : type === "LOVE" ? "bg-[#F33E58]" : ""}`}>
    {type === "LIKE" || type === "LOVE" ? <svg width={size * .65} height={size * .65} viewBox="0 0 24 24" fill="white">
      <path d={type === "LOVE" ? "M12 21l-1.45-1.32C5.4 15 2 11.91 2 8.12A5.45 5.45 0 017.5 2.6 6 6 0 0112 4.7a6 6 0 014.5-2.1A5.45 5.45 0 0122 8.12c0 3.79-3.4 6.88-8.55 11.57z" : "M7 10v11H3V10h4zm2 11V10l4-7c2 0 2 2 2 3l-1 4h5a2 2 0 012 2l-2 7a3 3 0 01-3 2H9z"} />
    </svg> : <Emoji unified={emoji[type]!} emojiStyle={EmojiStyle.FACEBOOK} size={size} />}
  </span>;
}

export default function ReactorsModal({ postId, onClose }: Props) {
  const [reactors, setReactors] = useState<ReactorView[]>([]);
  const [selected, setSelected] = useState<ReactionType | "ALL">("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [relationships, setRelationships] = useState<Map<string, Relationship> | null>(null);
  const [sending, setSending] = useState<Set<string>>(new Set());
  const [actionError, setActionError] = useState("");
  const [mutualCounts, setMutualCounts] = useState<Record<string, number>>({});
  const requestedDetails = useRef(new Set<string>());
  const pending = useRef(new Set<string>());
  const dialog = useRef<HTMLDialogElement>(null);
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialog.current?.showModal();
    return () => { document.body.style.overflow = overflow; previous?.focus(); };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true); setError(""); setReactors([]); setSelected("ALL");
    async function load() {
      try {
        const rows: PostReactor[] = [];
        // Include every page so tab counts and filtering aren't limited to the first 20 people.
        for (let page = 0; ; page++) {
          const batch = await fetchPostReactors(postId, page, 100);
          if (cancelled) return;
          rows.push(...batch);
          if (batch.length < 100) break;
        }
        const ids = [...new Set(rows.map(row => row.userId))];
        const profiles = new Map<string, UserSummary>();
        for (let offset = 0; offset < ids.length; offset += 100) {
          try {
            const response = await api.get<UserSummary[]>("/services/userservice/api/user-profiles/public", {
              params: { "id.in": ids.slice(offset, offset + 100).join(","), size: 100 },
            });
            response.data.forEach(profile => profiles.set(profile.id, profile));
          } catch { /* Keep reactions available when public profiles cannot be loaded. */ }
          if (cancelled) return;
        }
        setReactors(rows.map(row => ({ ...row,
          name: profiles.get(row.userId)?.displayName || profiles.get(row.userId)?.username || "Người dùng",
          avatar: getUserAvatarUrl(profiles.get(row.userId)),
        })));
      } catch { if (!cancelled) setError("Không tải được danh sách cảm xúc. Vui lòng thử lại."); }
      finally { if (!cancelled) setLoading(false); }
    }
    void load();
    return () => { cancelled = true; };
  }, [postId, attempt]);

  useEffect(() => {
    let cancelled = false;
    setRelationships(null);
    if (!user?.id) return;
    async function loadRelationships() {
      try {
        const readPages = async <T,>(url: string): Promise<T[]> => {
          const rows: T[] = [];
          for (let page = 0; ; page++) {
            const response = await api.get<T[]>(url, { params: { page, size: 100 } });
            rows.push(...response.data);
            if (cancelled || response.data.length < 100) return rows;
          }
        };
        const [friends, sent, received] = await Promise.all([
          readPages<FriendshipItem>("/services/userservice/api/friendships/me"),
          readPages<FriendRequestItem>("/services/userservice/api/friend-requests/sent"),
          readPages<FriendRequestItem>("/services/userservice/api/friend-requests/received"),
        ]);
        const map = new Map<string, Relationship>();
        sent.filter(r => r.status === "PENDING").forEach(r => map.set(r.receiver.id, "sent"));
        received.filter(r => r.status === "PENDING").forEach(r => map.set(r.sender.id, "received"));
        friends.forEach(f => map.set(f.user.id === user!.id ? f.friend.id : f.user.id, "friend"));
        if (!cancelled) setRelationships(map);
      } catch { /* Hide Add friend if the relationship could not be determined. */ }
    }
    void loadRelationships();
    return () => { cancelled = true; };
  }, [user?.id]);

  async function addFriend(id: string) {
    if (pending.current.has(id)) return;
    pending.current.add(id); setSending(new Set(pending.current)); setActionError("");
    try {
      await sendFriendRequest(id);
      setRelationships(previous => new Map(previous).set(id, "sent"));
    } catch { setActionError("Chưa gửi được lời mời kết bạn. Vui lòng thử lại."); }
    finally { pending.current.delete(id); setSending(new Set(pending.current)); }
  }

  const counts = Object.fromEntries(types.map(type => [type, reactors.filter(r => r.reactionType === type).length])) as Record<ReactionType, number>;
  const visible = selected === "ALL" ? reactors : reactors.filter(r => r.reactionType === selected);
  useEffect(() => {
    let cancelled = false;
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        const id = (entry.target as HTMLElement).dataset.reactorId;
        if (!entry.isIntersecting || !id || id === user?.id || requestedDetails.current.has(id)) return;
        requestedDetails.current.add(id);
        observer.unobserve(entry.target);
        void api.get<{ mutualFriendsCount?: number }>(`/services/userservice/api/user-profiles/${id}/details`)
          .then(({ data }) => {
            if (!cancelled && typeof data.mutualFriendsCount === "number") {
              setMutualCounts(previous => ({ ...previous, [id]: data.mutualFriendsCount! }));
            }
          }).catch(() => { /* Omit unavailable counts rather than inventing them. */ });
      });
    }, { root: dialog.current?.querySelector("[role=tabpanel]") });
    dialog.current?.querySelectorAll("[data-reactor-id]").forEach(row => observer.observe(row));
    return () => { cancelled = true; observer.disconnect(); requestedDetails.current.clear(); };
  }, [reactors, selected, user?.id]);
  function openProfile(id: string) { onClose(); navigate(`/profile/${id}`); }

  return createPortal(<dialog ref={dialog} aria-label="Người đã bày tỏ cảm xúc" onCancel={onClose}
    onKeyDown={event => event.stopPropagation()}
    onClick={event => {
      if (event.target !== event.currentTarget) return;
      const bounds = event.currentTarget.getBoundingClientRect();
      if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose();
    }}
    className="fixed inset-0 m-auto max-h-[calc(100dvh-32px)] w-[calc(100%-24px)] max-w-[548px] overflow-hidden rounded-lg bg-white p-0 text-[#1C1E21] shadow-xl backdrop:bg-black/40">
    <div className="flex h-[61px] items-center gap-2 border-b border-[#E4E6EB] px-3">
      <div role="tablist" aria-label="Lọc cảm xúc" className="flex h-full min-w-0 flex-1 overflow-x-auto">
        {(["ALL", ...types.filter(type => counts[type] > 0)] as const).map(type => <button key={type} type="button" role="tab" id={`reaction-tab-${type}`} aria-selected={selected === type} aria-controls="reaction-panel"
          aria-label={type === "ALL" ? "Tất cả" : `${labels[type]}: ${counts[type]}`}
          onClick={() => setSelected(type)}
          onKeyDown={event => {
            if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
            event.preventDefault();
            const tabs = Array.from(event.currentTarget.parentElement!.querySelectorAll<HTMLButtonElement>("[role=tab]"));
            const index = tabs.indexOf(event.currentTarget);
            const next = event.key === "Home" ? 0 : event.key === "End" ? tabs.length - 1 : (index + (event.key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
            tabs[next].focus(); tabs[next].click();
          }}
          tabIndex={selected === type ? 0 : -1}
          className={`flex h-full shrink-0 items-center justify-center gap-1 border-b-[3px] px-4 pt-[3px] text-sm font-semibold focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-[#1877F2] ${selected === type ? "border-[#1877F2] text-[#1877F2]" : "border-transparent text-[#65676B] hover:bg-[#F0F2F5]"}`}>
          {type === "ALL" ? "Tất cả" : <><ReactionIcon type={type} /><span>{counts[type]}</span></>}
        </button>)}
      </div>
      <button type="button" autoFocus aria-label="Đóng danh sách cảm xúc" onClick={onClose} className="flex size-9 shrink-0 items-center justify-center rounded-full bg-[#E4E6EB] hover:bg-[#D8DADF] focus-visible:outline-2 focus-visible:outline-[#1877F2]">
        <svg aria-hidden="true" className="size-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m6 6 12 12M6 18 18 6" /></svg>
      </button>
    </div>
    <div id="reaction-panel" role="tabpanel" aria-labelledby={`reaction-tab-${selected}`} aria-busy={loading} className="max-h-[min(60dvh,480px)] min-h-[320px] overflow-y-auto px-3 py-2">
      {loading && <UserRowsSkeleton count={5} label="Đang tải cảm xúc" />}
      {error && <div role="alert" className="p-6 text-center text-sm"><p>{error}</p><button type="button" onClick={() => setAttempt(a => a + 1)} className="mt-3 rounded-md bg-[#E4E6EB] px-4 py-2 font-semibold">Thử lại</button></div>}
      {!loading && !error && visible.length === 0 && <p className="p-6 text-center text-sm text-[#65676B]">Chưa có cảm xúc nào.</p>}
      {actionError && <p role="alert" className="px-1 py-2 text-sm text-[#B42318]">{actionError}</p>}
      {visible.map(reactor => {
        const relationship = relationships?.get(reactor.userId);
        return <div key={reactor.userId} data-reactor-id={reactor.userId} className="flex min-h-[52px] items-center gap-3 py-1.5">
          <button type="button" aria-label={`Trang cá nhân của ${reactor.name}`} onClick={() => openProfile(reactor.userId)} className="relative size-10 shrink-0 rounded-full focus-visible:outline-2 focus-visible:outline-[#1877F2]">
            <MediaImage src={reactor.avatar} alt="" className="size-10 rounded-full object-cover" />
            <span className="absolute bottom-0 right-0 flex size-5 items-center justify-center rounded-full border-2 border-white bg-white leading-none"><ReactionIcon type={reactor.reactionType} size={16} /></span>
          </button>
          <div className="min-w-0 flex-1">
            <button type="button" onClick={() => openProfile(reactor.userId)} className="block max-w-full truncate text-left text-sm font-semibold hover:underline focus-visible:outline-2 focus-visible:outline-[#1877F2]">{reactor.name}</button>
            {mutualCounts[reactor.userId] > 0 && <p className="mt-0.5 text-xs text-[#65676B]">{mutualCounts[reactor.userId]} bạn chung</p>}
          </div>
          {user?.id !== reactor.userId && <button type="button" disabled={sending.has(reactor.userId) || relationship === "sent"}
            onClick={() => { if (relationships && !relationship) void addFriend(reactor.userId); else openProfile(reactor.userId); }}
            className="flex min-h-9 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#E4E6EB] px-2.5 text-xs font-semibold hover:bg-[#D8DADF] focus-visible:outline-2 focus-visible:outline-[#1877F2] disabled:cursor-default disabled:text-[#65676B] sm:text-sm">
            {relationships && !relationship && <svg aria-hidden="true" className="size-4" viewBox="0 0 24 24" fill="currentColor"><path d="M9 12a4 4 0 100-8 4 4 0 000 8zm0 2c-4 0-7 2-7 5v2h12v-2c0-1.5.4-2.8 1.2-3.9C13.7 14.4 11.4 14 9 14zm11 0v3h3v2h-3v3h-2v-3h-3v-2h3v-3z" /></svg>}
            {sending.has(reactor.userId) ? "Đang gửi…" : relationship === "sent" ? "Đã gửi lời mời" : relationship === "received" ? "Phản hồi" : relationship === "friend" ? "Bạn bè" : relationships ? "Thêm bạn bè" : "Trang cá nhân"}
          </button>}
        </div>;
      })}
    </div>
  </dialog>, document.body);
}
