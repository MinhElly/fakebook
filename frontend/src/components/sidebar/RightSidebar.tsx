import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

export default function RightSidebar() {
  return <aside className="fixed bottom-0 right-0 top-14 hidden w-[360px] flex-col px-4 py-4 xl:flex"><h2 className="px-2 text-lg font-bold text-[#1C1E21]">Người liên hệ</h2><FeatureEmptyState compact title="Chưa có người liên hệ" description="Danh sách bạn bè đang hoạt động sẽ xuất hiện khi dữ liệu được kết nối." /></aside>;
}
