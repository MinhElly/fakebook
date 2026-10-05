import { useEffect, useState } from "react";
import { Link } from "react-router";
import api from "@/services/apis";
import { useAuth } from "@/providers/AuthProvider";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

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

  useEffect(() => {
    if (status !== "authenticated" || !user) return;
    
    let isMounted = true;

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
        console.error("Could not fetch contacts:", error);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchContacts();

    return () => {
      isMounted = false;
    };
  }, [status, user]);

  return (
    <aside className="fixed bottom-0 right-0 top-14 hidden w-[360px] flex-col px-4 py-4 xl:flex">
      <h2 className="px-2 text-lg font-bold text-[#1C1E21] mb-2">Người liên hệ</h2>
      {loading ? (
        <div className="flex justify-center py-4 text-sm text-gray-500">Đang tải...</div>
      ) : contacts.length > 0 ? (
        <div className="flex flex-col space-y-1">
          {contacts.map((contact) => (
            <Link
              key={contact.id}
              to={`/profile/${contact.id}`}
              className="flex items-center gap-3 rounded-lg p-2 hover:bg-gray-100 transition-colors"
            >
              <div className="relative">
                <img
                  src={contact.avatar}
                  alt={contact.name}
                  className="h-9 w-9 rounded-full object-cover border border-gray-200"
                />
                <div className="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-white bg-green-500"></div>
              </div>
              <span className="text-[15px] font-medium text-gray-900">{contact.name}</span>
            </Link>
          ))}
        </div>
      ) : (
        <FeatureEmptyState
          compact
          title="Chưa có người liên hệ"
          description="Danh sách bạn bè sẽ xuất hiện khi bạn kết bạn."
        />
      )}
    </aside>
  );
}
