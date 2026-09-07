import { useNavigate } from "react-router";
import { CONTACTS } from "@/constants/data";
import { useFriendStore } from "@/stores/friendStore";

export default function RightSidebar() {
  const navigate = useNavigate();
  const { pendingReceived, acceptRequest, rejectRequest } = useFriendStore();

  return (
    <aside
      className="
        hidden xl:flex flex-col
        w-[360px] fixed right-0 top-14 bottom-0
        overflow-y-auto px-4 py-4
      "
      style={{ scrollbarWidth: "none" }}
    >
      {pendingReceived.length > 0 && (
        <div className="mb-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="font-bold text-[#1C1E21] text-lg">Lời mời kết bạn</h3>
            <button
              onClick={() => navigate("/friends")}
              className="text-[#1877F2] hover:bg-blue-50 px-2 py-1 rounded text-sm font-semibold transition-colors"
            >
              Xem tất cả
            </button>
          </div>
          {pendingReceived.map(user => (
            <div key={user.id} className="flex items-start gap-2 mb-3">
              <img
                src={user.avatar}
                alt={user.name}
                onClick={() => navigate(`/profile/${user.id}`)}
                className="w-16 h-16 rounded-full object-cover flex-shrink-0 cursor-pointer hover:opacity-90 transition-opacity"
              />
              <div className="flex-1 min-w-0">
                <p
                  onClick={() => navigate(`/profile/${user.id}`)}
                  className="font-semibold text-[#1C1E21] text-sm hover:underline cursor-pointer"
                >
                  {user.name}
                </p>
                <p className="text-[#65676B] text-xs mb-2">{user.mutualFriends} bạn chung</p>
                <div className="flex gap-2">
                  <button onClick={() => acceptRequest(user.id)} className="flex-1 bg-[#1877F2] hover:bg-[#166FE5] text-white text-sm font-semibold py-1.5 rounded-lg transition-colors">Xác nhận</button>
                  <button onClick={() => rejectRequest(user.id)} className="flex-1 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold py-1.5 rounded-lg transition-colors">Xóa</button>
                </div>
              </div>
            </div>
          ))}
          <hr className="border-[#E4E6EB] mb-4" />
        </div>
      )}

      <div>
        <h3 className="font-bold text-[#1C1E21] text-lg mb-2">Người liên hệ</h3>
        {CONTACTS.map(contact => (
          <button key={contact.id} className="flex items-center gap-3 w-full p-2 rounded-lg hover:bg-[#F0F2F5] transition-colors">
            <div className="relative flex-shrink-0">
              <img src={contact.avatar} alt={contact.name} className="w-9 h-9 rounded-full object-cover" />
              {contact.online && <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-white" />}
            </div>
            <span className="font-medium text-[#1C1E21] text-sm">{contact.name}</span>
          </button>
        ))}
      </div>

      <hr className="border-[#E4E6EB] my-4" />

      <div className="text-xs text-[#65676B] leading-relaxed">
        <div className="flex flex-wrap gap-x-2 gap-y-1 mb-2">
          {["Quyền riêng tư", "Điều khoản", "Quảng cáo", "Cookie", "Xem thêm"].map(l => (
            <a key={l} href="#" className="hover:underline">{l}</a>
          ))}
        </div>
        <p>Meta © 2025</p>
      </div>
    </aside>
  );
}
