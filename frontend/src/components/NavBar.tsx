import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function NavBar() {
  const { user, signOut } = useAuth();
  if (!user) return null;

  return (
    <nav className="topnav">
      <div className="topnav-inner">
        <div className="row">
          <Link to="/dashboard" className="topnav-brand">
            <strong>Princess Project</strong>
          </Link>
          <span className="muted">{user.nickname}</span>
        </div>
        <div className="topnav-links">
          <NavLink to="/record" className={({ isActive }) => (isActive ? "link active" : "link")}>
            오늘 기록
          </NavLink>
          <NavLink to="/dashboard" className={({ isActive }) => (isActive ? "link active" : "link")}>
            대시보드
          </NavLink>
          <NavLink to="/stat-focus" className={({ isActive }) => (isActive ? "link active" : "link")}>
            나의 목표
          </NavLink>
          <NavLink to="/my-page" className={({ isActive }) => (isActive ? "link active" : "link")}>
            마이페이지
          </NavLink>
          {user.role === "ADMIN" && (
            <NavLink to="/admin" className={({ isActive }) => (isActive ? "link active" : "link")}>
              관리자
            </NavLink>
          )}
          <a href="#" className="link" onClick={(e) => { e.preventDefault(); signOut(); }}>
            로그아웃
          </a>
        </div>
      </div>
    </nav>
  );
}
