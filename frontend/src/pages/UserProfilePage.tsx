import { useNavigate } from "react-router";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

export default function UserProfilePage() {
  const navigate = useNavigate();
  return <main className="min-h-[calc(100vh-56px)] bg-[#F0F2F5] px-4 py-8"><div className="mx-auto max-w-[720px] rounded-xl border border-[#E4E6EB] bg-white"><FeatureEmptyState title="Chưa thể tải hồ sơ người dùng" description="Tính năng xem hồ sơ người khác cần dữ liệu từ user service và hiện chưa được kết nối." /><div className="flex justify-center border-t border-[#E4E6EB] px-4 py-3"><button onClick={() => navigate(-1)} className="rounded-lg bg-[#E4E6EB] px-4 py-2 text-sm font-semibold text-[#1C1E21] transition-colors hover:bg-[#D8DADF] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1877F2]">Quay lại</button></div></div></main>;
}
