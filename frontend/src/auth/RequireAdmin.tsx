import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

/**
 * Client-side convenience only - the real gate is the backend's ROLE_ADMIN check on
 * /api/admin/**. This just avoids flashing the admin UI at non-admins before their API
 * calls come back with 403s.
 */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { token, user } = useAuth();
  if (!token || !user) return <Navigate to="/login" replace />;
  if (user.role !== "ADMIN") return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}
