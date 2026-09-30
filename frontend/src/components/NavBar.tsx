import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ENDING_VISITED_EVENT, useEndingStatus, visitedEndingToday } from "../lib/ending";

export function NavBar() {
  const { user, signOut } = useAuth();
  const { status } = useEndingStatus(!!user);
  const [, setVisitTick] = useState(0);

  useEffect(() => {
    const onVisited = () => setVisitTick((n) => n + 1);
    window.addEventListener(ENDING_VISITED_EVENT, onVisited);
    return () => window.removeEventListener(ENDING_VISITED_EVENT, onVisited);
  }, []);

  if (!user) return null;

  // 공개 기간(10/1~)에는 그날 엔딩 페이지에 처음 들어가기 전까지 N 뱃지 노출
  const showNewBadge = status?.phase === "REVEALED" && !visitedEndingToday(user.id);

  return (
    <nav className="topnav">
      <div className="topnav-inner">
        <div className="row">
          <Link to="/dashboard" className="topnav-brand">
            <strong>Princess Diary</strong>
          </Link>
          <span className="muted">{user.nickname}</span>
        </div>
        <div className="topnav-links">
          <NavLink to="/record" className={({ isActive }) => (isActive ? "link active" : "link")}>
            오늘 기록
          </NavLink>
          {/* 주간 회고는 매일 쓰는 오늘 기록과 달리 주 1회만 작성하면 되는 공통 과제라, 매일 보는
              /record 목록에 섞여 있으면 매번 스쳐 지나가기 쉽다 - 상단 메뉴에 별도 항목으로 분리해서
              주 1회 진입/트래킹 지점을 명확히 한다 (2026-08-21 요청). */}
          <NavLink to="/weekly-retrospective" className={({ isActive }) => (isActive ? "link active" : "link")}>
            주간 회고
          </NavLink>
          {/* 예전엔 /record 페이지 안에서 그날그날 일회성으로만 보이던 걸, 지금까지 쌓인 전체
              대화를 채팅처럼 쭉 볼 수 있게 상단 메뉴로 뺐다 (2026-08-26 요청). */}
          <NavLink to="/butler" className={({ isActive }) => (isActive ? "link active" : "link")}>
            레오집사
          </NavLink>
          <NavLink to="/dashboard" className={({ isActive }) => (isActive ? "link active" : "link")}>
            대시보드
          </NavLink>
          {/* 공주 엔딩 (2026-09 화면설계서 v0.2) - 대시보드 다음, 로그인 가능 기간 상시 노출.
              10/1 전에는 공개 전 페이지, 10/1~10/20엔 엔딩 페이지를 같은 경로에서 분기한다. */}
          <NavLink to="/ending" className={({ isActive }) => (isActive ? "link active" : "link")}>
            엔딩 확인하기
            {showNewBadge && (
              <span className="nav-new-badge" aria-label="새 소식">
                N
              </span>
            )}
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
