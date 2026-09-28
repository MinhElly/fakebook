import { RouterProvider } from "react-router";
import { router } from "./routes";
import AuthProvider from "@/providers/AuthProvider";
import PostProvider from "@/providers/PostProvider";
import UserProvider from "@/providers/UserProvider";
import CommentProvider from "@/providers/CommentProvider";
import FriendProvider from "@/providers/FriendProvider";
import RealtimeProvider from "@/providers/RealtimeProvider";

export default function App() {
  return (
    <AuthProvider>
      <RealtimeProvider>
        <UserProvider>
          <PostProvider>
            <CommentProvider>
              <FriendProvider>
                <RouterProvider router={router} />
              </FriendProvider>
            </CommentProvider>
          </PostProvider>
        </UserProvider>
      </RealtimeProvider>
    </AuthProvider>
  );
}
