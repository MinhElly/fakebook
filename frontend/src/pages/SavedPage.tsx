import { PostSkeleton } from "@/components/ui/Skeleton";
import LoadError from "@/components/ui/LoadError";
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
  const { status, user } = useAuth();
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    if (status !== "authenticated") { setSavedPosts([]); return; }

    const fetchSaved = async () => {
      try {
        setLoading(true); setError(false);
        const res = await api.get("/services/postservice/api/posts/saved");
        const postsData = res.data || [];

        const hydrated = await hydratePosts(postsData);
        if (active) setSavedPosts(hydrated);
      } catch (error) {
        if (active) setError(true);
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchSaved();
    return () => { active = false; };
  }, [status, user?.id, retry]);

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
          
          {error && <LoadError message="Không tải được bài viết đã lưu." onRetry={() => setRetry(n => n + 1)} />}
          {loading && savedPosts.length === 0 ? <PostSkeleton /> : savedPosts.length > 0 ? (
            savedPosts.map((post) => (
              <Post key={post.id} post={post} />
            ))
          ) : !error && !loading && (
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
