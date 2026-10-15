import { useAuth } from "@/providers/AuthProvider"

export default function useRequireLogin() {
  const { status, login } = useAuth()

  return () => {
    if (status === "authenticated") return true

    void login()

    return false
  }
}
