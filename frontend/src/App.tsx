import { useEffect } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { NavBar } from "./components/NavBar";
import { Footer } from "./components/Footer";
import { Mascot } from "./components/Mascot";
import { BubbleEffect } from "./components/BubbleEffect";
import { OnboardingBridge } from "./components/OnboardingBridge";
import { RequireAuth } from "./auth/RequireAuth";
import { RequireAdmin } from "./auth/RequireAdmin";
import { useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { ForgotPasswordPage } from "./pages/ForgotPasswordPage";
import { ResetPasswordPage } from "./pages/ResetPasswordPage";
import { RecordPage } from "./pages/RecordPage";
import { WeeklyRetrospectivePage } from "./pages/WeeklyRetrospectivePage";
import { ButlerChatPage } from "./pages/ButlerChatPage";
import { DashboardPage } from "./pages/DashboardPage";
import { StatFocusPage } from "./pages/StatFocusPage";
import { MyPage } from "./pages/MyPage";
import { AdminPage } from "./pages/AdminPage";
import { initAnalytics, trackPageView } from "./lib/analytics";

function App() {
  const { token } = useAuth();
  const location = useLocation();

  useEffect(() => {
    initAnalytics();
  }, []);

  // Fires on every route change (including the first) - a plain gtag config only ever
  // reports the initial load in an SPA, since there's no full page reload afterwards.
  useEffect(() => {
    trackPageView(location.pathname + location.search);
  }, [location]);

  // React Router keeps the document's scroll position between routes. The butler history is
  // intentionally scrollable and can leave the document far below the top; without resetting,
  // the next page (and especially a modal opened there) appears displaced relative to the user's
  // viewport. Each app screen should start from its own original top-aligned layout.
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [location.pathname]);

  return (
    <div className="app-shell">
      <BubbleEffect />
      {/* 카톡/인스타 초대 링크로 들어온 미가입 방문자용 스토리텔링 브릿지 팝업.
          별도 URL 없이 로그인 화면 위에만 뜬다 (2026-08-13 온보딩 회의 결정사항). */}
      {!token && location.pathname === "/login" && <OnboardingBridge />}
      <NavBar />
      <Routes>
        <Route path="/login" element={token ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
        {/* Ungated (not wrapped in RequireAuth, not gated on token) - a signed-in user
            landing on a stale reset-link tab shouldn't get bounced away either. */}
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route
          path="/record"
          element={
            <RequireAuth>
              <RecordPage />
            </RequireAuth>
          }
        />
        <Route
          path="/weekly-retrospective"
          element={
            <RequireAuth>
              <WeeklyRetrospectivePage />
            </RequireAuth>
          }
        />
        <Route
          path="/butler"
          element={
            <RequireAuth>
              <ButlerChatPage />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard"
          element={
            <RequireAuth>
              <DashboardPage />
            </RequireAuth>
          }
        />
        <Route
          path="/stat-focus"
          element={
            <RequireAuth>
              <StatFocusPage />
            </RequireAuth>
          }
        />
        <Route
          path="/my-page"
          element={
            <RequireAuth>
              <MyPage />
            </RequireAuth>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <AdminPage />
            </RequireAdmin>
          }
        />
        <Route path="*" element={<Navigate to={token ? "/dashboard" : "/login"} replace />} />
      </Routes>
      <Footer />
      <Mascot />
    </div>
  );
}

export default App;
