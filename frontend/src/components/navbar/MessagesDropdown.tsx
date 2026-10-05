import Dropdown from "@/components/ui/Dropdown";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

export default function MessagesDropdown() {
  return <Dropdown className="w-[360px]"><div className="border-b border-[#E4E6EB] px-4 py-3"><h2 className="text-xl font-bold text-[#1C1E21]">Tin nhắn</h2></div><FeatureEmptyState compact title="Chưa có cuộc trò chuyện" description="Tin nhắn sẽ xuất hiện khi dịch vụ Messenger được kết nối." /></Dropdown>;
}
