import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { AuthContext, type AuthStatus, type UserProfile } from "@/stores/authStore";
import keycloak from "@/services/keycloak";

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
  const initialized = useRef(false);

  const syncAuthenticatedState = useCallback(() => {
    if (!keycloak.authenticated || !keycloak.token) {
      setStatus("unauthenticated");
      setUser(null);
      return;
    }

    setStatus("authenticated");
    setUser(profileFromToken());
    setError(null);
  }, []);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    keycloak.onAuthSuccess = syncAuthenticatedState;
    keycloak.onAuthRefreshSuccess = syncAuthenticatedState;
    keycloak.onAuthLogout = () => {
      setStatus("unauthenticated");
      setUser(null);
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

  const redirectUri = `${window.location.origin}/`;

  return (
    <AuthContext.Provider
      value={{
        status,
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
