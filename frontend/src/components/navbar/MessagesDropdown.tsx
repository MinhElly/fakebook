import Dropdown from "@/components/ui/Dropdown";
import { MESSAGES } from "@/constants/data";

export default function MessagesDropdown() {
  return (
    <Dropdown className="w-[360px]">
      <div className="px-4 pt-4 pb-2 flex items-center justify-between">
        <h2 className="text-xl font-bold text-[#1C1E21]">Tin nhắn</h2>
        <button className="w-9 h-9 rounded-full bg-[#F0F2F5] hover:bg-[#E4E6EB] flex items-center justify-center text-[#1877F2] transition-colors">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" strokeLinejoin="round" strokeLinecap="round"/></svg>
        </button>
      </div>

      <div className="px-3 pb-2">
        <div className="flex items-center gap-2 bg-[#F0F2F5] rounded-full px-3 h-9">
          <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd"/></svg>
          <input className="bg-transparent outline-none text-sm flex-1 text-[#1C1E21] placeholder-[#65676B]" placeholder="Tìm kiếm trên Messenger" />
        </div>
      </div>

      <div className="px-3 pb-1 flex gap-2">
        {["Hộp thư đến", "Đoạn chat"].map((t, i) => (
          <button key={t} className={`px-3 py-1.5 rounded-full text-sm font-semibold transition-colors ${i === 0 ? "bg-[#E7F3FF] text-[#1877F2]" : "text-[#65676B] hover:bg-[#F0F2F5]"}`}>{t}</button>
        ))}
      </div>

      <div className="max-h-[400px] overflow-y-auto" style={{ scrollbarWidth: "none" }}>
        {MESSAGES.map((m) => (
          <button key={m.id} className="flex items-center gap-3 w-full px-3 py-2 hover:bg-[#F0F2F5] transition-colors">
            <div className="relative flex-shrink-0">
              <img src={m.avatar} alt={m.name} className="w-14 h-14 rounded-full object-cover" />
              {m.online && <span className="absolute bottom-0.5 right-0.5 w-3.5 h-3.5 bg-green-500 rounded-full border-2 border-white" />}
            </div>
            <div className="flex-1 min-w-0 text-left">
              <p className={`text-sm truncate ${m.unread ? "font-bold text-[#1C1E21]" : "font-semibold text-[#1C1E21]"}`}>{m.name}</p>
              <p className={`text-xs truncate ${m.unread ? "font-semibold text-[#1C1E21]" : "text-[#65676B]"}`}>{m.text} · {m.time}</p>
            </div>
            {m.unread > 0 && (
              <span className="flex-shrink-0 bg-[#1877F2] text-white text-xs font-bold rounded-full min-w-[20px] h-5 flex items-center justify-center px-1">{m.unread}</span>
            )}
          </button>
        ))}
      </div>

      <div className="p-3 border-t border-[#E4E6EB]">
        <button className="w-full text-center text-[#1877F2] text-sm font-semibold hover:bg-[#F0F2F5] py-2 rounded-lg transition-colors">
          Mở Messenger
        </button>
      </div>
    </Dropdown>
  );
}
