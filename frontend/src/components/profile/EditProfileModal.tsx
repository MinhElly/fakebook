import { useState } from "react";
import { useUserStore, type UserProfile } from "@/stores/userStore";

interface Props {
  onClose: () => void;
}

type Tab = "info" | "avatar" | "cover";

const RELATIONSHIP_OPTIONS = ["Độc thân", "Đã kết hôn", "Đang hẹn hò", "Đã đính hôn", "Phức tạp", "Không muốn nói"];

export default function EditProfileModal({ onClose }: Props) {
  const { profile, updateProfile } = useUserStore();
  const [tab, setTab] = useState<Tab>("info");
  const [form, setForm] = useState<Omit<UserProfile, "avatar" | "cover">>({
    name: profile.name,
    location: profile.location,
    education: profile.education,
    work: profile.work,
    relationship: profile.relationship,
    bio: profile.bio,
  });
  const [avatarUrl, setAvatarUrl] = useState(profile.avatar);
  const [coverUrl, setCoverUrl] = useState(profile.cover);
  const [avatarPreview, setAvatarPreview] = useState(profile.avatar);
  const [coverPreview, setCoverPreview] = useState(profile.cover);

  function update(k: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
      setForm(prev => ({ ...prev, [k]: e.target.value }));
  }

  function handleSave() {
    updateProfile({ ...form, avatar: avatarUrl.trim() || profile.avatar, cover: coverUrl.trim() || profile.cover });
    onClose();
  }

  const inputCls = "w-full border border-[#CED0D4] rounded-lg px-3 h-11 text-sm outline-none focus:border-[#1877F2] text-[#1C1E21] placeholder-[#90949C] transition-colors";
  const labelCls = "block text-sm font-semibold text-[#1C1E21] mb-1.5";

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-[560px] mx-4 max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-[#E4E6EB] flex-shrink-0">
          <div className="w-9" />
          <h2 className="font-bold text-[#1C1E21] text-lg">Chỉnh sửa trang cá nhân</h2>
          <button onClick={onClose} className="w-9 h-9 rounded-full bg-[#E4E6EB] hover:bg-[#D8DADF] flex items-center justify-center transition-colors">
            <svg className="w-5 h-5 text-[#1C1E21]" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd"/></svg>
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-[#E4E6EB] flex-shrink-0">
          {([["info", "Thông tin"], ["avatar", "Ảnh đại diện"], ["cover", "Ảnh bìa"]] as [Tab, string][]).map(([t, label]) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex-1 py-3 text-sm font-semibold transition-colors ${tab === t ? "text-[#1877F2] border-b-[3px] border-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"}`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Body */}
        <div className="overflow-y-auto flex-1 px-4 py-4 space-y-4" style={{ scrollbarWidth: "none" }}>
          {tab === "info" && (
            <>
              <div>
                <label className={labelCls}>Họ và tên</label>
                <input className={inputCls} value={form.name} onChange={update("name")} placeholder="Nhập họ và tên" />
              </div>
              <div>
                <label className={labelCls}>Tiểu sử</label>
                <textarea
                  className="w-full border border-[#CED0D4] rounded-lg px-3 py-2.5 text-sm outline-none focus:border-[#1877F2] text-[#1C1E21] placeholder-[#90949C] resize-none transition-colors"
                  rows={3}
                  value={form.bio}
                  onChange={update("bio")}
                  placeholder="Mô tả bản thân..."
                  maxLength={101}
                />
                <p className="text-xs text-[#65676B] text-right mt-0.5">{form.bio.length}/101</p>
              </div>
              <div>
                <label className={labelCls}>
                  <span className="flex items-center gap-2">🏠 Nơi sống</span>
                </label>
                <input className={inputCls} value={form.location} onChange={update("location")} placeholder="Thành phố, Quốc gia" />
              </div>
              <div>
                <label className={labelCls}>
                  <span className="flex items-center gap-2">🎓 Học vấn</span>
                </label>
                <input className={inputCls} value={form.education} onChange={update("education")} placeholder="Trường học, Đại học..." />
              </div>
              <div>
                <label className={labelCls}>
                  <span className="flex items-center gap-2">💼 Nơi làm việc</span>
                </label>
                <input className={inputCls} value={form.work} onChange={update("work")} placeholder="Công ty, vị trí..." />
              </div>
              <div>
                <label className={labelCls}>
                  <span className="flex items-center gap-2">❤️ Tình trạng quan hệ</span>
                </label>
                <select
                  className={`${inputCls} cursor-pointer`}
                  value={form.relationship}
                  onChange={update("relationship")}
                >
                  {RELATIONSHIP_OPTIONS.map(opt => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
              </div>
            </>
          )}

          {tab === "avatar" && (
            <div className="space-y-4">
              <div className="flex flex-col items-center gap-4">
                <img
                  src={avatarPreview}
                  alt="avatar preview"
                  className="w-32 h-32 rounded-full object-cover border-4 border-[#E4E6EB]"
                  onError={(e) => (e.currentTarget.src = profile.avatar)}
                />
                <p className="text-sm text-[#65676B]">Xem trước ảnh đại diện</p>
              </div>
              <div>
                <label className={labelCls}>URL ảnh đại diện mới</label>
                <input
                  className={inputCls}
                  value={avatarUrl}
                  onChange={(e) => {
                    setAvatarUrl(e.target.value);
                    setAvatarPreview(e.target.value);
                  }}
                  placeholder="Dán URL ảnh vào đây..."
                />
              </div>
            </div>
          )}

          {tab === "cover" && (
            <div className="space-y-4">
              <div>
                <img
                  src={coverPreview}
                  alt="cover preview"
                  className="w-full h-32 object-cover rounded-xl border border-[#E4E6EB]"
                  onError={(e) => (e.currentTarget.src = profile.cover)}
                />
                <p className="text-sm text-[#65676B] mt-1 text-center">Xem trước ảnh bìa</p>
              </div>
              <div>
                <label className={labelCls}>URL ảnh bìa mới</label>
                <input
                  className={inputCls}
                  value={coverUrl}
                  onChange={(e) => {
                    setCoverUrl(e.target.value);
                    setCoverPreview(e.target.value);
                  }}
                  placeholder="Dán URL ảnh bìa vào đây..."
                />
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-4 py-3 border-t border-[#E4E6EB] flex gap-2 flex-shrink-0">
          <button onClick={onClose} className="flex-1 h-10 rounded-lg bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold text-sm transition-colors">
            Hủy
          </button>
          <button onClick={handleSave} className="flex-1 h-10 rounded-lg bg-[#1877F2] hover:bg-[#166FE5] text-white font-bold text-sm transition-colors">
            Lưu thay đổi
          </button>
        </div>
      </div>
    </div>
  );
}
