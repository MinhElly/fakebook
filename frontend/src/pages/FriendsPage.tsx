import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

export default function FriendsPage() {
  return <main className="min-h-[calc(100vh-56px)] bg-[#F0F2F5] px-4 py-6 sm:px-6"><div className="mx-auto max-w-[940px]"><div className="mb-4"><h1 className="text-2xl font-bold text-[#1C1E21]">Bạn bè</h1><p className="mt-1 text-sm text-[#65676B]">Quản lý bạn bè, lời mời và gợi ý kết nối.</p></div><section className="rounded-xl border border-[#E4E6EB] bg-white"><FeatureEmptyState title="Chưa có dữ liệu bạn bè" description="Danh sách bạn bè, lời mời kết bạn và gợi ý sẽ xuất hiện tại đây sau khi user service được kết nối." /></section></div></main>;
}
