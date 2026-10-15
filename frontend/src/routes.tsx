import RequireAuth from "@/components/RequireAuth";
import { createBrowserRouter, Navigate } from "react-router";
import MainLayout from "@/layouts/MainLayout";
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
    Component: MainLayout,
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
  { path: "/login", Component: LoginPage },
  { path: "/register", Component: RegisterPage },
]);
