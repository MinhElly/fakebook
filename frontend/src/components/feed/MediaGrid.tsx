import MediaImage from "./MediaImage";
import { getMediaUrl } from "@/services/mediaService";

interface MediaGridProps {
  mediaIds?: string[];
  images?: (string | null)[];
  onOpen: (index: number) => void;
}

export default function MediaGrid({ mediaIds = [], images = [], onOpen }: MediaGridProps) {
  const mediaList = (mediaIds.length ? mediaIds : images.filter((image): image is string => !!image)).map(getMediaUrl);
  if (!mediaList.length) return null;
  const count = mediaList.length;
  return (
    <div className={`mt-3 grid overflow-hidden bg-black/5 ${count === 1 ? "grid-cols-1" : count === 3 ? "grid-cols-2 grid-rows-2 gap-1" : "grid-cols-2 gap-1"}`}>
      {mediaList.slice(0, 4).map((url, index) => (
        <button key={`${url}-${index}`} type="button" aria-label={`Mở ảnh ${index + 1}`}
          onClick={() => onOpen(index)}
          className={`relative block min-w-0 cursor-pointer overflow-hidden rounded-none bg-black/10 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-[#1877F2] ${
            count === 1 ? "max-h-[500px]" : count === 2 ? "h-[300px]" : count === 3 ? (index === 0 ? "row-span-2 h-[320px]" : "h-[158px]") : "h-[180px]"
          }`}>
          <MediaImage src={url} alt={`Ảnh bài viết ${index + 1}`} draggable={false} className={`w-full rounded-none object-cover ${count === 1 ? "h-auto" : "h-full"}`} />
          {index === 3 && count > 4 && <span className="absolute inset-0 flex items-center justify-center bg-black/60 text-2xl font-bold text-white">+{count - 4}</span>}
        </button>
      ))}
    </div>
  );
}
