import { createContext, useContext } from "react";

export type AuthStatus = "initializing" | "authenticated" | "unauthenticated" | "error";

export interface UserProfile {
  id?: string;
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
}

export interface AuthState {
  roles: string[];
  accountKind: "ADMIN" | "USER" | null;
  isAdmin: boolean;
  status: AuthStatus;
  user: UserProfile | null;
  error: string | null;
  login: () => Promise<void>;
  register: () => Promise<void>;
  resetPassword: () => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthState>({
  roles: [],
  accountKind: null,
  isAdmin: false,
  status: "initializing",
  user: null,
  error: null,
  login: async () => {},
  register: async () => {},
  resetPassword: async () => {},
  logout: async () => {},
});

export function useAuthStore() {
  return useContext(AuthContext);
}
