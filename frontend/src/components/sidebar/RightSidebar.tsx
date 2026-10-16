import { UserRowsSkeleton } from "@/components/ui/Skeleton";
import LoadError from "@/components/ui/LoadError";
import MediaImage from "@/components/feed/MediaImage";
import { useEffect, useState } from "react";
import { Link } from "react-router";
import api from "@/services/apis";
import { useAuth } from "@/providers/AuthProvider";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";
import PresenceBadge from "@/components/ui/PresenceBadge";
import { usePresences } from "@/providers/PresenceProvider";
import { UNKNOWN_PRESENCE } from "@/services/presenceService";
import PresenceText from "@/components/ui/PresenceText";

interface Contact {
  id: string;
  name: string;
  avatar: string;
  avatarMediaId?: string;
}

export default function RightSidebar() {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [loading, setLoading] = useState(true);
  const { status, user } = useAuth();
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [visible, setVisible] = useState(() => window.matchMedia("(min-width: 1280px)").matches);
  const { entries, connected } = usePresences(visible ? contacts.map(contact => contact.id) : []);
  useEffect(() => {
    const query = window.matchMedia("(min-width: 1280px)");
    const changed = () => setVisible(query.matches);
    query.addEventListener("change", changed);
    return () => query.removeEventListener("change", changed);
  }, []);

  useEffect(() => {
    if (status !== "authenticated" || !user) return;
    
    let isMounted = true;
    setLoading(true); setError(false);

    const fetchContacts = async () => {
      try {
        const res = await api.get("/services/userservice/api/friendships/me?size=50");
        const friendships = res.data || [];
        
        if (friendships.length === 0) {
          if (isMounted) setContacts([]);
          return;
        }

        const mappedContacts = friendships.map((f: any) => {
          const otherProfile = f.friend?.id === user.id ? f.user : f.friend;
          return {
            id: otherProfile.id,
            name: otherProfile.displayName || otherProfile.username || "Người dùng",
            avatarMediaId: otherProfile.avatarMediaId,
            avatar: "/default-avatar.svg"
          };
        });

        // Fetch avatars
        const avatarIds = mappedContacts.map((c: Contact) => c.avatarMediaId).filter(Boolean);
        if (avatarIds.length > 0) {
          try {
            const mediaRes = await api.get(`/services/mediaservice/api/media?id.in=${avatarIds.join(",")}`);
            const mediaMap: Record<string, string> = {};
            mediaRes.data.forEach((m: any) => {
              mediaMap[m.id] = m.url;
            });
            mappedContacts.forEach((c: Contact) => {
              if (c.avatarMediaId && mediaMap[c.avatarMediaId]) {
                c.avatar = mediaMap[c.avatarMediaId];
              }
            });
          } catch (error) {
            console.warn("Could not fetch media for avatars");
          }
        }

        if (isMounted) {
          setContacts(mappedContacts);
        }
      } catch (error) {
        if (isMounted) setError(true);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchContacts();

    return () => {
      isMounted = false;
    };
  }, [status, user?.id, attempt]);

  return (
    <aside className="fixed bottom-0 right-0 top-14 hidden w-[360px] flex-col px-4 py-4 xl:flex">
      <h2 className="px-2 text-lg font-bold text-[#1C1E21] mb-2">Người liên hệ</h2>
      {!connected && contacts.length > 0 && <p role="status" className="px-2 mb-2 text-xs text-[#65676B]">Đang kết nối trạng thái hoạt động…</p>}
      {error && <LoadError message="Không tải được người liên hệ." onRetry={() => setAttempt(n => n + 1)} />}
      {loading && contacts.length === 0 ? <UserRowsSkeleton /> : contacts.length > 0 ? (
        <div className="flex flex-col space-y-1 overflow-y-auto">
          {contacts.map((contact) => (
            <Link
              key={contact.id}
              to={`/profile/${contact.id}`}
              className="flex items-center gap-3 rounded-lg p-2 hover:bg-gray-100 transition-colors"
            >
              <div className="relative">
                <MediaImage
                  src={contact.avatar}
                  alt={contact.name}
                  className="h-9 w-9 rounded-full object-cover border border-gray-200"
                />
                  <PresenceBadge status={entries[contact.id]?.status} />
              </div>
              <span className="min-w-0 text-[15px] font-medium text-gray-900"><span className="block truncate">{contact.name}</span><PresenceText presence={entries[contact.id] || UNKNOWN_PRESENCE} /></span>
            </Link>
          ))}
        </div>
      ) : !error && !loading && (
        <FeatureEmptyState
          compact
          title="Chưa có người liên hệ"
          description="Danh sách bạn bè sẽ xuất hiện khi bạn kết bạn."
        />
      )}
    </aside>
  );
}
