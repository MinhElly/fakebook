import { useRef } from "react";
import { STORIES } from "@/constants/data";
import { useUserStore } from "@/stores/userStore";

export default function Stories() {
  const { profile } = useUserStore();
  const scrollRef = useRef<HTMLDivElement>(null);

  function scrollRight() {
    scrollRef.current?.scrollBy({ left: 300, behavior: "smooth" });
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] p-3 mb-3 relative">
      <div ref={scrollRef} className="flex gap-2 overflow-x-auto" style={{ scrollbarWidth: "none" }}>
        {/* Tạo tin card */}
        <div className="relative flex-shrink-0 w-[115px] h-[200px] rounded-xl overflow-hidden cursor-pointer group border border-[#E4E6EB]">
          {/* Top 2/3: gray bg with avatar */}
          <div className="w-full bg-[#E4E6EB] group-hover:bg-[#D8DADF] transition-colors" style={{ height: "68%" }}>
            <img
              src={profile.avatar}
              alt="me"
              className="w-full h-full object-cover"
            />
          </div>
          {/* Bottom 1/3: white with + button */}
          <div className="absolute bottom-0 left-0 right-0 bg-white flex flex-col items-center pt-3 pb-2" style={{ height: "38%" }}>
            <div className="absolute -top-4 w-9 h-9 bg-[#1877F2] rounded-full flex items-center justify-center border-4 border-white shadow-sm">
              <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clipRule="evenodd"/>
              </svg>
            </div>
            <p className="text-[#1C1E21] text-xs font-semibold mt-2">Tạo tin</p>
          </div>
        </div>

        {/* Other stories */}
        {STORIES.filter(s => !s.isOwn).map((story) => (
          <div key={story.id} className="relative flex-shrink-0 w-[115px] h-[200px] rounded-xl overflow-hidden cursor-pointer group">
            <img
              src={story.bg}
              alt={story.name}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
            />
            {/* gradient overlay */}
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/10" />
            {/* Avatar top-left with blue ring */}
            <div className="absolute top-2 left-2">
              <div className="rounded-full p-[2px] bg-[#1877F2]">
                <img
                  src={story.avatar}
                  alt={story.name}
                  className="w-9 h-9 rounded-full object-cover border-2 border-white"
                />
              </div>
            </div>
            {/* Name bottom-left */}
            <p className="absolute bottom-2 left-2 right-2 text-white text-xs font-semibold leading-tight drop-shadow">
              {story.name}
            </p>
          </div>
        ))}
      </div>

      {/* Scroll right arrow */}
      <button
        onClick={scrollRight}
        className="absolute right-1 top-1/2 -translate-y-1/2 w-9 h-9 bg-white rounded-full shadow-md border border-[#E4E6EB] flex items-center justify-center hover:bg-[#F0F2F5] transition-colors z-10"
      >
        <svg className="w-4 h-4 text-[#1C1E21]" fill="currentColor" viewBox="0 0 24 24">
          <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
        </svg>
      </button>
    </div>
  );
}
