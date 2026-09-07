import LeftSidebar from "@/components/sidebar/LeftSidebar";
import RightSidebar from "@/components/sidebar/RightSidebar";
import Stories from "@/components/feed/Stories";
import PostCreator from "@/components/feed/PostCreator";
import Post from "@/components/feed/Post";
import { usePostStore } from "@/stores/postStore";
import FeatureEmptyState from "@/components/ui/FeatureEmptyState";

export default function HomePage() {
  const { posts } = usePostStore();

  return (
    <div className="flex bg-[#F0F2F5] min-h-screen">
      <LeftSidebar />

      {/*
        Margins match sidebar widths at each breakpoint:
          mobile  (<768px):  no sidebars  → ml-0   mr-0
          tablet  (768px+):  left 72px    → ml-[72px]  mr-0
          laptop  (1024px+): left 280px   → ml-[280px] mr-0
          desktop (1280px+): left+right 360px each
      */}
      <main className="
        flex-1 min-w-0 py-4 px-3 sm:px-4
        ml-0 md:ml-[72px] lg:ml-[280px] xl:ml-[360px]
        mr-0 xl:mr-[360px]
      ">
        <Stories />
        <PostCreator />
        {posts.length === 0 && (
          <section className="rounded-xl border border-[#E4E6EB] bg-white">
            <FeatureEmptyState title="Chưa có bài viết" description="Bảng tin sẽ hiển thị bài viết sau khi post service được kết nối." />
          </section>
        )}
        {posts.map(post => (
          <Post key={post.id} post={post} />
        ))}
      </main>

      <RightSidebar />
    </div>
  );
}
