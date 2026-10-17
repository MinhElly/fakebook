import RequireAuth from "@/components/RequireAuth";
import { createBrowserRouter, Navigate } from "react-router";
import CommunityLayout from "@/layouts/CommunityLayout";
import AdminLayout from "@/layouts/AdminLayout";
import RequireAdmin from "@/components/RequireAdmin";
import AdminPage from "@/pages/admin/AdminPage";
import HomePage from "@/pages/HomePage";
import ProfilePage from "@/pages/ProfilePage";
import FriendsPage from "@/pages/FriendsPage";
import UserProfilePage from "@/pages/UserProfilePage";
import SearchPage from "@/pages/SearchPage";
import LoginPage from "@/pages/LoginPage";
import RegisterPage from "@/pages/RegisterPage";
import SavedPage from "@/pages/SavedPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <CommunityLayout />,
    children: [
      { index: true, Component: HomePage },
      { path: "feed/friends", element: <Navigate to="/" replace /> },
      { path: "profile", element: <RequireAuth><ProfilePage /></RequireAuth> },
      { path: "friends", element: <RequireAuth><FriendsPage /></RequireAuth> },
      { path: "search", element: <RequireAuth><SearchPage /></RequireAuth> },
      { path: "profile/:userId", Component: UserProfilePage },
      { path: "saved", element: <RequireAuth><SavedPage /></RequireAuth> },
    ],
  },
  {
    path: "/admin",
    element: <RequireAuth><RequireAdmin><AdminLayout /></RequireAdmin></RequireAuth>,
    children: [
      { index: true, element: <AdminPage area="overview" /> },
      { path: "users", element: <AdminPage area="users" /> },
      { path: "admins", element: <AdminPage area="admins" /> },
      { path: "posts", element: <AdminPage area="posts" /> },
      { path: "comments", element: <AdminPage area="comments" /> },
      { path: "audit", element: <AdminPage area="audit" /> },
    ],
  },
  { path: "/login", Component: LoginPage },
  { path: "/register", Component: RegisterPage },
]);
