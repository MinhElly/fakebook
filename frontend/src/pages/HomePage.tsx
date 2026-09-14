import LeftSidebar from "@/components/sidebar/LeftSidebar";
import RightSidebar from "@/components/sidebar/RightSidebar";
import Stories from "@/components/feed/Stories";
import PostCreator from "@/components/feed/PostCreator";
import Post from "@/components/feed/Post";
import { usePostStore } from "@/stores/postStore";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";
import Toast from "@/components/ui/Toast";
import { useEffect } from "react";

export default function HomePage() {
  const { posts, loadMorePosts, hasMore, loading, isUploading, toastMessage, setToastMessage } = usePostStore();

  // Bắt sự kiện cuộn chuột để làm Infinity Scroll
  useEffect(() => {
    const handleScroll = () => {
      if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 200) {
        if (hasMore && !loading) {
          loadMorePosts();
        }
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [hasMore, loading]);

  return (
    <div className="flex bg-[#F0F2F5] min-h-screen">
      <LeftSidebar />

      <main className="
            flex-1 min-w-0 py-4 px-3 sm:px-4
            ml-0 md:ml-[72px] lg:ml-[280px] xl:ml-[360px]
            mr-0 xl:mr-[360px]
          ">
        <Stories />
        <PostCreator />

        {/* Uploading Placeholder */}
        {isUploading && (
          <div className="bg-white rounded-xl shadow border border-[#E4E6EB] mb-4 overflow-hidden relative opacity-70">
            {/* Progress bar */}
            <div className="h-1 w-full bg-gray-200 absolute top-0 left-0">
              <div className="h-full bg-blue-500 animate-pulse w-2/3 rounded-r-full"></div>
            </div>
            <div className="p-4 flex items-center gap-2">
               <div className="w-10 h-10 rounded-full bg-gray-300 animate-pulse"></div>
               <div className="flex flex-col gap-2">
                 <div className="w-32 h-3 bg-gray-300 rounded animate-pulse"></div>
                 <div className="w-20 h-2 bg-gray-200 rounded animate-pulse"></div>
               </div>
            </div>
            <div className="px-4 pb-4">
               <div className="w-full h-3 bg-gray-300 rounded animate-pulse mb-2"></div>
               <div className="w-2/3 h-3 bg-gray-300 rounded animate-pulse"></div>
            </div>
          </div>
        )}

        {posts.length === 0 && !loading && (
          <section className="rounded-xl border border-[#E4E6EB] bg-white">
            <FeatureEmptyState title="Chưa có bài viết" description="Bảng tin sẽ hiển thị bài viết sau khi bạn đăng bài." />
          </section>
        )}

        {posts.map(post => (
          <Post key={post.id} post={post} />
        ))}

        {/* Hiển thị vòng xoay đang tải hoặc thông báo hết bài */}
        {loading && (
          <div className="text-center py-6 text-[#65676B] font-semibold text-sm">
            Đang tải thêm bài viết...
          </div>
        )}
        {!hasMore && posts.length > 0 && (
          <div className="text-center py-6 text-[#65676B] text-sm">
            Bạn đã xem hết bài viết!
          </div>
        )}
      </main>

      <RightSidebar />

      {/* Toast Notification */}
      {toastMessage && (
        <Toast 
          message={toastMessage} 
          type={toastMessage.includes("Lỗi") ? "error" : "success"} 
          onClose={() => setToastMessage(null)} 
        />
      )}
    </div>
  );
}