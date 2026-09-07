import { useUserStore } from "@/stores/userStore";

export default function PostCreator() {
  const { profile } = useUserStore();
  return (
    <div className="mb-3 flex items-center gap-3 rounded-xl border border-[#E4E6EB] bg-white px-4 py-3">
      <img src={profile.avatar} alt="" className="h-10 w-10 flex-shrink-0 rounded-full object-cover" />
      <button disabled className="h-10 flex-1 cursor-not-allowed rounded-full bg-[#F0F2F5] px-4 text-left text-sm text-[#65676B]" title="Cần kết nối post service để đăng bài">
        Tạo bài viết sẽ khả dụng sau khi kết nối post service
      </button>
    </div>
  );
}
