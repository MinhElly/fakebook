import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { AuthContext, type AuthStatus, type UserProfile } from "@/stores/authStore";
import keycloak from "@/services/keycloak";
import { getRuntimeConfig } from "@/config/runtime-config";

export function useAuth() {
  return useContext(AuthContext);
}

function profileFromToken(): UserProfile | null {
  const claims = keycloak.tokenParsed;
  if (!claims) return null;

  return {
    id: claims.sub,
    username: claims.preferred_username as string | undefined,
    email: claims.email as string | undefined,
    firstName: claims.given_name as string | undefined,
    lastName: claims.family_name as string | undefined,
  };
}

export default function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("initializing");
  const [user, setUser] = useState<UserProfile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [accountKind, setAccountKind] = useState<"ADMIN" | "USER" | null>(null);
  const generation = useRef(0);
  const initialized = useRef(false);

  useEffect(() => {
    const block = (event: Event) => {
      generation.current++;
      setStatus("error"); setUser(null); setRoles([]); setAccountKind(null);
      setError((event as CustomEvent).detail === "ACCOUNT_SUSPENDED" ? "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên." : "Phiên đăng nhập đã bị thu hồi. Vui lòng đăng nhập lại.");
    };
    window.addEventListener("fakebook:account-blocked", block);
    return () => window.removeEventListener("fakebook:account-blocked", block);
  }, []);

  const syncAuthenticatedState = useCallback(async () => {
    const current = ++generation.current;
    if (!keycloak.authenticated || !keycloak.token) {
      setStatus("unauthenticated");
      setUser(null);
      setRoles([]); setAccountKind(null);
      return;
    }

    const claims = keycloak.tokenParsed;
    const tokenRoles = [...new Set([...(claims?.roles as string[] || []), ...(claims?.realm_access?.roles || [])])];
    try {
      const response = await fetch(`${getRuntimeConfig().apiBaseUrl || ""}/services/userservice/api/account-state`, {
        headers: { Authorization: `Bearer ${keycloak.token}` }, cache: "no-store",
      });
      if (!response.ok) throw new Error(response.status === 403 ? "Tài khoản bị hạn chế hoặc phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại." : "Không thể kiểm tra trạng thái tài khoản. Vui lòng thử lại.");
      const state = await response.json();
      if (state.accountKind !== "USER" && state.accountKind !== "ADMIN") throw new Error("Không xác định được loại tài khoản.");
      if (current !== generation.current) return;
      setRoles(tokenRoles); setAccountKind(state.accountKind);
      setStatus("authenticated"); setUser(profileFromToken()); setError(null);
    } catch (cause) {
      if (current !== generation.current) return;
      setStatus("error"); setUser(null); setRoles([]); setAccountKind(null);
      setError(cause instanceof Error ? cause.message : "Không thể kiểm tra trạng thái tài khoản.");
    }
  }, []);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    keycloak.onAuthSuccess = syncAuthenticatedState;
    keycloak.onAuthRefreshSuccess = syncAuthenticatedState;
    keycloak.onAuthLogout = () => {
      generation.current++;
      setStatus("unauthenticated");
      setUser(null);
      setRoles([]); setAccountKind(null);
    };
    keycloak.onAuthError = () => {
      setStatus("error");
      setError("Không thể hoàn tất xác thực với Keycloak.");
    };
    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).then(syncAuthenticatedState).catch(() => {
        keycloak.clearToken();
        setStatus("unauthenticated");
        setUser(null);
      });
    };

    keycloak
      .init({
        onLoad: "check-sso",
        pkceMethod: "S256",
        checkLoginIframe: false,
      })
      .then(syncAuthenticatedState)
      .catch((cause: unknown) => {
        console.error("Keycloak initialization failed", cause);
        setStatus("error");
        setError("Không thể kết nối tới dịch vụ đăng nhập.");
      });
  }, [syncAuthenticatedState]);

  const redirectUri = ["/login", "/register"].includes(window.location.pathname) ? `${window.location.origin}/` : window.location.href;

  return (
    <AuthContext.Provider
      value={{
        status,
        roles,
        accountKind,
        isAdmin: accountKind === "ADMIN",
        user,
        error,
        login: () => keycloak.login({ redirectUri }),
        register: () => keycloak.register({ redirectUri }),
        resetPassword: () => keycloak.login({ redirectUri, action: "UPDATE_PASSWORD" }),
        logout: () => keycloak.logout({ redirectUri: window.location.origin }),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
