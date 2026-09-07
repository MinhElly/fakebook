import { RouterProvider } from "react-router";
import { router } from "./routes";
import AuthProvider from "@/providers/AuthProvider";
import PostProvider from "@/providers/PostProvider";
import UserProvider from "@/providers/UserProvider";
import CommentProvider from "@/providers/CommentProvider";
import FriendProvider from "@/providers/FriendProvider";

export default function App() {
  return (
    <AuthProvider>
      <UserProvider>
        <PostProvider>
          <CommentProvider>
            <FriendProvider>
              <RouterProvider router={router} />
            </FriendProvider>
          </CommentProvider>
        </PostProvider>
      </UserProvider>
    </AuthProvider>
  );
}
