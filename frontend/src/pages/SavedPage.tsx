import { useEffect, useState } from "react";
import LeftSidebar from "@/components/sidebar/LeftSidebar";
import RightSidebar from "@/components/sidebar/RightSidebar";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";
import api from "@/services/apis";
import Post from "@/components/feed/Post";
import { useAuth } from "@/providers/AuthProvider";
import { hydratePosts } from "@/utils/postHydration";

export default function SavedPage() {
  const [savedPosts, setSavedPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const { status } = useAuth();

  useEffect(() => {
    if (status !== "authenticated") return;

    const fetchSaved = async () => {
      try {
        setLoading(true);
        const res = await api.get("/services/postservice/api/posts/saved");
        const postsData = res.data || [];

        const hydrated = await hydratePosts(postsData);
        setSavedPosts(hydrated);
      } catch (error) {
        console.error("Lỗi khi tải bài viết đã lưu:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchSaved();
  }, [status]);

  return (
    <div className="flex bg-[#F0F2F5] min-h-screen">
      <LeftSidebar />

      <main className="
            flex-1 min-w-0 py-4 px-3 sm:px-4
            ml-0 md:ml-[72px] lg:ml-[280px] xl:ml-[360px]
            mr-0 xl:mr-[360px]
          ">
        <section className="rounded-xl bg-transparent mt-4 mb-4">
          <h1 className="text-2xl font-bold mb-4 px-2">Bài viết đã lưu</h1>
          
          {loading ? (
            <div className="text-center py-6 text-[#65676B] font-semibold text-sm">
              Đang tải...
            </div>
          ) : savedPosts.length > 0 ? (
            savedPosts.map((post) => (
              <Post key={post.id} post={post} />
            ))
          ) : (
            <div className="bg-white rounded-xl border border-[#E4E6EB] p-4">
              <FeatureEmptyState 
                title="Chưa có bài viết nào được lưu" 
                description="Các bài viết bạn đã lưu sẽ xuất hiện ở đây." 
              />
            </div>
          )}
        </section>
      </main>

      <RightSidebar />
    </div>
  );
}
