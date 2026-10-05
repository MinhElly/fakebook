import { useEffect } from "react";
import Post from "./Post";
import type { Post as PostType } from "@/types";

interface PostDetailModalProps {
  post: PostType;
  onClose: () => void;
}

export default function PostDetailModal({ post, onClose }: PostDetailModalProps) {
  useEffect(() => {
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "unset";
    };
  }, []);

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 sm:p-6"
      onClick={onClose}
    >
      <button
        onClick={onClose}
        className="fixed top-4 left-4 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white z-[60] transition-colors"
      >
        <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
        </svg>
      </button>

      <div 
        className="relative w-full max-w-[680px] max-h-full bg-[#F0F2F5] sm:rounded-xl shadow-2xl overflow-y-auto no-scrollbar"
        onClick={(e) => e.stopPropagation()}
      >
        {/* We use the Post component but tell it it's in a modal to show comments by default and disable modal-triggering */}
        <Post post={post} isModal={true} />
      </div>
    </div>
  );
}
