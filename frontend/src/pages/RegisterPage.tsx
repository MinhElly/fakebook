import { useEffect, useRef } from "react";
import { Navigate } from "react-router";
import { useAuth } from "@/providers/AuthProvider";

export default function RegisterPage() {
  const { status, error, register } = useAuth();
  const redirectStarted = useRef(false);

  useEffect(() => {
    if (status !== "unauthenticated" || redirectStarted.current) return;

    redirectStarted.current = true;
    void register();
  }, [register, status]);

  if (status === "authenticated") return <Navigate to="/" replace />;

  if (status === "error") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-fb-bg px-4">
        <div className="w-full max-w-[420px] rounded-xl bg-white p-6 text-center shadow-lg">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-fb-blue text-3xl font-bold text-white">
            f
          </div>
          <h1 className="text-xl font-bold text-fb-text">Không thể mở trang đăng ký</h1>
          <p role="alert" className="mt-2 text-sm leading-5 text-red-700">
            {error ?? "Dịch vụ xác thực hiện không phản hồi. Vui lòng thử lại."}
          </p>
          <button
            type="button"
            onClick={() => {
              redirectStarted.current = true;
              void register();
            }}
            className="mt-5 h-11 w-full rounded-lg bg-fb-blue font-bold text-white transition-colors hover:bg-fb-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-fb-blue"
          >
            Thử đăng ký lại
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-fb-bg" aria-busy="true">
      <p className="sr-only">Đang mở trang đăng ký…</p>
    </main>
  );
}
