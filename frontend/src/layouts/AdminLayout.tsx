import { NavLink, Outlet } from "react-router";
import { useAuth } from "@/providers/AuthProvider";
import "@/pages/admin/admin.css";

const navigation = [
  ["/admin", "Tổng quan"], ["/admin/users", "Người dùng"], ["/admin/admins", "Quản trị viên"],
  ["/admin/posts", "Bài viết"], ["/admin/comments", "Bình luận"], ["/admin/audit", "Nhật ký"],
];

export default function AdminLayout() {
  const { user, logout } = useAuth();
  return <div className="admin-shell">
    <a className="admin-skip" href="#admin-content">Đến nội dung</a>
    <aside className="admin-sidebar" aria-label="Điều hướng quản trị">
      <div className="admin-brand"><span aria-hidden="true">f</span><div>Fakebook<small>Quản trị hệ thống</small></div></div>
      <nav>{navigation.map(([path, label]) => <NavLink key={path} to={path} end={path === "/admin"}>{label}</NavLink>)}</nav>
      <p className="admin-sidebar-note">Tài khoản quản trị chỉ dùng để quản lý và kiểm duyệt.</p>
    </aside>
    <div className="admin-workspace">
      <header className="admin-topbar"><span>{user?.username || "Quản trị viên"}</span><button onClick={() => void logout()}>Đăng xuất</button></header>
      <main id="admin-content" className="admin-content" tabIndex={-1}><Outlet /></main>
    </div>
  </div>;
}
