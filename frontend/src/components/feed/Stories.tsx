import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

export default function Stories() {
  return <section className="mb-3 rounded-xl border border-[#E4E6EB] bg-white" aria-label="Tin"><FeatureEmptyState compact title="Chưa có tin để hiển thị" description="Tin sẽ xuất hiện tại đây sau khi dịch vụ stories được kết nối." /></section>;
}
