import { createContext, useContext } from "react";

export interface AuthState {
  isLoggedIn: boolean;
  login: () => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthState>({
  isLoggedIn: false,
  login: () => {},
  logout: () => {},
});

export function useAuthStore() {
  return useContext(AuthContext);
}
