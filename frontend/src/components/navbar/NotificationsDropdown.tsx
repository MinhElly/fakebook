import Dropdown from "@/components/ui/Dropdown";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

export default function NotificationsDropdown() {
  return <Dropdown className="w-[380px]"><div className="border-b border-[#E4E6EB] px-4 py-3"><h2 className="text-xl font-bold text-[#1C1E21]">Thông báo</h2></div><FeatureEmptyState compact title="Chưa có thông báo" description="Thông báo mới sẽ xuất hiện sau khi dịch vụ thông báo được kết nối." /></Dropdown>;
}
