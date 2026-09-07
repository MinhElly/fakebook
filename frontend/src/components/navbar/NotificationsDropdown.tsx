import { useState } from "react";
import Dropdown from "@/components/ui/Dropdown";
import { NOTIFICATIONS } from "@/constants/data";
import { getNotifIcon } from "@/utils/notifIcon";

export default function NotificationsDropdown() {
  const [notifs, setNotifs] = useState(NOTIFICATIONS);

  function markRead(id: number) {
    setNotifs(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
  }

  return (
    <Dropdown className="w-[380px]">
      <div className="px-4 pt-4 pb-2 flex items-center justify-between">
        <h2 className="text-xl font-bold text-[#1C1E21]">Thông báo</h2>
        <button className="w-9 h-9 rounded-full bg-[#F0F2F5] hover:bg-[#E4E6EB] flex items-center justify-center text-[#65676B] transition-colors">
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20"><path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z"/></svg>
        </button>
      </div>

      <div className="px-3 pb-2 flex gap-2">
        {["Tất cả", "Chưa đọc"].map((t, i) => (
          <button key={t} className={`px-3 py-1.5 rounded-full text-sm font-semibold transition-colors ${i === 0 ? "bg-[#E7F3FF] text-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"}`}>{t}</button>
        ))}
      </div>

      <div className="max-h-[440px] overflow-y-auto" style={{ scrollbarWidth: "none" }}>
        <p className="px-4 py-1 text-[#1C1E21] font-semibold text-sm">Mới</p>
        {notifs.map((n) => (
          <button
            key={n.id}
            onClick={() => markRead(n.id)}
            className={`flex items-center gap-3 w-full px-3 py-2 hover:bg-[#F0F2F5] transition-colors ${!n.read ? "bg-[#EBF5FF]" : ""}`}
          >
            <div className="relative flex-shrink-0">
              <img src={n.avatar} alt="" className="w-14 h-14 rounded-full object-cover" />
              <span className="absolute -bottom-0.5 -right-0.5 bg-[#1877F2] rounded-full w-6 h-6 flex items-center justify-center border-2 border-white text-sm">
                {getNotifIcon(n.text)}
              </span>
            </div>
            <div className="flex-1 min-w-0 text-left">
              <p className={`text-sm leading-snug ${!n.read ? "font-semibold text-[#1C1E21]" : "text-[#1C1E21]"}`}>{n.text}</p>
              <p className={`text-xs mt-0.5 ${!n.read ? "text-[#1877F2] font-semibold" : "text-[#65676B]"}`}>{n.time}</p>
            </div>
            {!n.read && <span className="flex-shrink-0 w-3 h-3 rounded-full bg-[#1877F2]" />}
          </button>
        ))}
      </div>

      <div className="p-3 border-t border-[#E4E6EB]">
        <button className="w-full text-center text-[#1877F2] text-sm font-semibold hover:bg-[#F0F2F5] py-2 rounded-lg transition-colors">
          Xem tất cả thông báo
        </button>
      </div>
    </Dropdown>
  );
}
