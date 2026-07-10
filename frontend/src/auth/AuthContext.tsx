import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { UserResponse } from "../api/types";

interface AuthState {
  token: string | null;
  user: UserResponse | null;
  signIn: (token: string, user: UserResponse) => void;
  updateUser: (user: UserResponse) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

function readStoredUser(): UserResponse | null {
  const raw = localStorage.getItem("princess_user");
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserResponse;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(localStorage.getItem("princess_token"));
  const [user, setUser] = useState<UserResponse | null>(readStoredUser());

  const value = useMemo<AuthState>(
    () => ({
      token,
      user,
      signIn: (newToken, newUser) => {
        localStorage.setItem("princess_token", newToken);
        localStorage.setItem("princess_user", JSON.stringify(newUser));
        setToken(newToken);
        setUser(newUser);
      },
      updateUser: (newUser) => {
        localStorage.setItem("princess_user", JSON.stringify(newUser));
        setUser(newUser);
      },
      signOut: () => {
        localStorage.removeItem("princess_token");
        localStorage.removeItem("princess_user");
        setToken(null);
        setUser(null);
      },
    }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
