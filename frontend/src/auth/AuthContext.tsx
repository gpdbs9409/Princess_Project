import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { getUser } from "../api/endpoints";
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

  // The stored user is a snapshot taken at sign-in, so any server-side change made after that
  // (role promotion via ADMIN_NICKNAMES, cohort assignment) stays invisible until the next
  // login - which is how an admin could end up bounced off /admin by a cached role: "USER".
  // Re-fetch once on mount so a returning session always reflects the current server state.
  useEffect(() => {
    const storedId = readStoredUser()?.id;
    if (!localStorage.getItem("princess_token") || !storedId) return;
    getUser(storedId)
      .then((fresh) => {
        localStorage.setItem("princess_user", JSON.stringify(fresh));
        setUser(fresh);
      })
      .catch(() => {
        // offline or expired token - the 401 handler in the API client already signs out,
        // and any other failure just leaves the cached snapshot in place
      });
  }, []);

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
