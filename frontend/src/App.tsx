import { useEffect } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { NavBar } from "./components/NavBar";
import { Footer } from "./components/Footer";
import { Mascot } from "./components/Mascot";
import { BubbleEffect } from "./components/BubbleEffect";
import { RequireAuth } from "./auth/RequireAuth";
import { RequireAdmin } from "./auth/RequireAdmin";
import { useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { RecordPage } from "./pages/RecordPage";
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

  return (
    <div className="app-shell">
      <BubbleEffect />
      <NavBar />
      <Routes>
        <Route path="/login" element={token ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
        <Route
          path="/record"
          element={
            <RequireAuth>
              <RecordPage />
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
