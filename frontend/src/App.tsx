import { Navigate, Route, Routes } from "react-router-dom";
import { NavBar } from "./components/NavBar";
import { Footer } from "./components/Footer";
import { RequireAuth } from "./auth/RequireAuth";
import { useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { RecordPage } from "./pages/RecordPage";
import { DashboardPage } from "./pages/DashboardPage";
import { StatFocusPage } from "./pages/StatFocusPage";

function App() {
  const { token } = useAuth();

  return (
    <div className="app-shell">
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
        <Route path="*" element={<Navigate to={token ? "/dashboard" : "/login"} replace />} />
      </Routes>
      <Footer />
    </div>
  );
}

export default App;
