import { useState } from "react";
import { useNavigate, Navigate } from "react-router";
import { useAuth } from "@/providers/AuthProvider";
import { validateLogin } from "@/schemas/authSchema";

export default function LoginPage() {
  const navigate = useNavigate();
  const { isLoggedIn, login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  if (isLoggedIn) return <Navigate to="/" replace />;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const err = validateLogin({ email, password });
    if (err) { setError(err); return; }
    login();
    navigate("/");
  }

  return (
    <div className="min-h-screen bg-[#F0F2F5] flex items-center justify-center px-4" style={{ fontFamily: "'Inter', system-ui, sans-serif" }}>
      <div className="flex flex-col lg:flex-row items-center gap-8 max-w-[980px] w-full">
        {/* Left */}
        <div className="flex-1 flex flex-col items-start">
          <svg width="240" height="60" viewBox="0 0 240 60" fill="none" className="-ml-4">
            <circle cx="30" cy="30" r="30" fill="#1877F2"/>
            <path d="M40 19h-4.5c-1.65 0-3 1.35-3 3v3h7.5l-1.05 7.5H32.5V50h-7.5V32.5h-4.5V25h4.5v-3c0-4.97 4.03-9 9-9H40v6z" fill="white"/>
            <text x="68" y="44" fontFamily="Inter, system-ui, sans-serif" fontWeight="700" fontSize="36" fill="#1877F2">facebook</text>
          </svg>
          <p className="text-2xl text-[#1C1E21] mt-4 leading-relaxed max-w-[420px]">
            Facebook giúp bạn kết nối và chia sẻ với mọi người trong cuộc sống của bạn.
          </p>
        </div>

        {/* Right */}
        <div className="w-full max-w-[396px]">
          <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-lg p-4 space-y-3">
            <input
              type="text"
              placeholder="Email hoặc số điện thoại"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full border border-[#CED0D4] rounded-lg px-4 h-14 text-base outline-none focus:border-[#1877F2] text-[#1C1E21] placeholder-[#90949C]"
            />
            <input
              type="password"
              placeholder="Mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border border-[#CED0D4] rounded-lg px-4 h-14 text-base outline-none focus:border-[#1877F2] text-[#1C1E21] placeholder-[#90949C]"
            />
            {error && <p className="text-red-500 text-sm">{error}</p>}
            <button type="submit" className="w-full bg-[#1877F2] hover:bg-[#166FE5] text-white text-xl font-bold h-14 rounded-lg transition-colors">
              Đăng nhập
            </button>
            <p className="text-center">
              <button type="button" className="text-[#1877F2] text-sm hover:underline">Quên mật khẩu?</button>
            </p>
            <hr className="border-[#E4E6EB]" />
            <div className="flex justify-center pb-1">
              <button
                type="button"
                onClick={() => navigate("/register")}
                className="bg-[#42B72A] hover:bg-[#36A420] text-white text-base font-bold px-5 py-3 rounded-lg transition-colors"
              >
                Tạo tài khoản mới
              </button>
            </div>
          </form>
          <p className="text-center mt-4 text-sm text-[#1C1E21]">
            Tạo Trang dành cho{" "}
            <a href="#" className="font-semibold hover:underline">người nổi tiếng, ban nhạc hoặc doanh nghiệp.</a>
          </p>
        </div>
      </div>
    </div>
  );
}
