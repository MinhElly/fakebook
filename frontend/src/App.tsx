import { RouterProvider } from "react-router"

import { router } from "./routes"

import { useAuth } from "@/providers/AuthProvider"

import AuthProvider from "@/providers/AuthProvider"

import PostProvider from "@/providers/PostProvider"

import UserProvider from "@/providers/UserProvider"

import CommentProvider from "@/providers/CommentProvider"

import FriendProvider from "@/providers/FriendProvider"

import RealtimeProvider from "@/providers/RealtimeProvider"

export default function App() {
  return (
    <AuthProvider>
      <DataProviders />
    </AuthProvider>
  )
}

function DataProviders() {
  const { status, user } = useAuth()

  return (
    <RealtimeProvider key={`${status}:${user?.id || "guest"}`}>
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
  )
}
