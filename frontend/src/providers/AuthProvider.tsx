import { useState, useContext } from "react";
import { AuthContext } from "@/stores/authStore";

export function useAuth() {
  return useContext(AuthContext);
}

export default function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isLoggedIn, setIsLoggedIn] = useState(
    () => localStorage.getItem("fb_auth") === "true"
  );

  function login() {
    localStorage.setItem("fb_auth", "true");
    setIsLoggedIn(true);
  }

  function logout() {
    localStorage.removeItem("fb_auth");
    setIsLoggedIn(false);
  }

  return (
    <AuthContext.Provider value={{ isLoggedIn, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
