import { useState } from "react";
import { useNavigate } from "react-router";
import { useAuth } from "@/providers/AuthProvider";
import { validateRegister, type RegisterFields } from "@/schemas/authSchema";

const EMPTY: RegisterFields = { firstName: "", lastName: "", email: "", password: "", birthday: "", gender: "" };

const INPUT_CLS = "w-full border border-[#CED0D4] rounded-lg px-3 h-11 text-sm outline-none focus:border-[#1877F2] text-[#1C1E21] placeholder-[#90949C] bg-white";

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState<RegisterFields>(EMPTY);
  const [error, setError] = useState("");

  function update(k: keyof RegisterFields) {
    return (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const err = validateRegister(form);
    if (err) { setError(err); return; }
    login();
    navigate("/");
  }

  return (
    <div className="min-h-screen bg-[#F0F2F5] flex items-center justify-center px-4 py-8" style={{ fontFamily: "'Inter', system-ui, sans-serif" }}>
      <div className="bg-white rounded-xl shadow-lg w-full max-w-[432px] overflow-hidden">
        <div className="p-4">
          <div className="flex items-center justify-between mb-1">
            <div>
              <h1 className="text-3xl font-bold text-[#1C1E21]">Tạo tài khoản mới</h1>
              <p className="text-[#65676B] text-sm mt-0.5">Nhanh chóng và dễ dàng.</p>
            </div>
            <button onClick={() => navigate("/login")} className="text-[#65676B] hover:bg-[#F0F2F5] w-9 h-9 rounded-full flex items-center justify-center transition-colors text-xl">×</button>
          </div>

          <hr className="border-[#E4E6EB] mb-3" />

          <form onSubmit={handleSubmit} className="space-y-2.5">
            <div className="flex gap-2">
              <input className={INPUT_CLS} placeholder="Họ" value={form.firstName} onChange={update("firstName")} />
              <input className={INPUT_CLS} placeholder="Tên" value={form.lastName} onChange={update("lastName")} />
            </div>
            <input className={INPUT_CLS} placeholder="Số điện thoại di động hoặc email" value={form.email} onChange={update("email")} />
            <input type="password" className={INPUT_CLS} placeholder="Mật khẩu mới" value={form.password} onChange={update("password")} />

            <div>
              <p className="text-xs text-[#65676B] mb-1.5">Ngày sinh</p>
              <input type="date" className={`${INPUT_CLS} w-full`} value={form.birthday} onChange={update("birthday")} />
            </div>

            <div>
              <p className="text-xs text-[#65676B] mb-1.5">Giới tính</p>
              <div className="flex gap-2">
                {[{ value: "female", label: "Nữ" }, { value: "male", label: "Nam" }, { value: "other", label: "Tùy chỉnh" }].map(({ value, label }) => (
                  <label key={value} className={`flex-1 border rounded-lg px-3 h-11 flex items-center justify-between cursor-pointer text-sm font-medium transition-colors ${form.gender === value ? "border-[#1877F2] bg-[#E7F3FF]" : "border-[#CED0D4] hover:bg-[#F0F2F5]"}`}>
                    {label}
                    <input type="radio" name="gender" value={value} className="accent-[#1877F2]" onChange={update("gender")} />
                  </label>
                ))}
              </div>
            </div>

            {error && <p className="text-red-500 text-xs">{error}</p>}

            <p className="text-xs text-[#65676B] leading-relaxed">
              Bằng cách nhấp vào Đăng ký, bạn đồng ý với{" "}
              <a href="#" className="text-[#1877F2] hover:underline">Điều khoản</a>,{" "}
              <a href="#" className="text-[#1877F2] hover:underline">Chính sách quyền riêng tư</a> và{" "}
              <a href="#" className="text-[#1877F2] hover:underline">Chính sách cookie</a> của chúng tôi.
            </p>

            <div className="flex justify-center pt-1 pb-3">
              <button type="submit" className="bg-[#42B72A] hover:bg-[#36A420] text-white text-base font-bold px-16 py-2.5 rounded-lg transition-colors">
                Đăng ký
              </button>
            </div>
          </form>

          <p className="text-center pb-2">
            <button onClick={() => navigate("/login")} className="text-[#1877F2] text-sm font-semibold hover:underline">
              Bạn đã có tài khoản?
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
