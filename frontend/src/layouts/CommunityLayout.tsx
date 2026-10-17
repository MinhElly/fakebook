import MainLayout from "@/layouts/MainLayout";
import RequireAuth from "@/components/RequireAuth";
import { RequireCommunity } from "@/components/RequireAdmin";
import PostProvider from "@/providers/PostProvider";
import UserProvider from "@/providers/UserProvider";
import CommentProvider from "@/providers/CommentProvider";
import FriendProvider from "@/providers/FriendProvider";
import RealtimeProvider from "@/providers/RealtimeProvider";
import PresenceProvider from "@/providers/PresenceProvider";

export default function CommunityLayout() {
  return <RequireAuth><RequireCommunity>
    <RealtimeProvider><PresenceProvider><UserProvider><PostProvider><CommentProvider><FriendProvider>
      <MainLayout />
    </FriendProvider></CommentProvider></PostProvider></UserProvider></PresenceProvider></RealtimeProvider>
  </RequireCommunity></RequireAuth>;
}
