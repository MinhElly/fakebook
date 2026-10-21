import MediaImage from "./MediaImage";
import MediaVideo from "./MediaVideo";
import { getMediaUrl } from "@/services/mediaService";

interface MediaGridProps {
  mediaIds?: string[];
  images?: (string | null)[];
  media?: { id: string; mediaType: string }[];
  onOpen: (index: number) => void;
}

export default function MediaGrid({ mediaIds = [], images = [], media = [], onOpen }: MediaGridProps) {
  const mediaList = (mediaIds.length ? mediaIds : images.filter((image): image is string => !!image)).map(getMediaUrl);
  if (!mediaList.length) return null;
  const count = mediaList.length;
  if (count === 1 && media.find(item => item.id === mediaIds[0])?.mediaType === "VIDEO") return <div className="relative mt-3">
    <MediaVideo mediaId={mediaIds[0]} autoPlayOnVisible onExpand={() => onOpen(0)} className="aspect-video w-full max-h-[560px]" />
  </div>;
  return (
    <div className={`mt-3 grid overflow-hidden bg-black/5 ${count === 1 ? "grid-cols-1" : count === 3 ? "grid-cols-2 grid-rows-2 gap-1" : "grid-cols-2 gap-1"}`}>
      {mediaList.slice(0, 5).map((url, index) => (
        <button key={`${url}-${index}`} type="button" aria-label={`Mở tệp ${index + 1}`}
          onClick={() => onOpen(index)}
          className={`relative block min-w-0 cursor-pointer overflow-hidden rounded-none bg-black/10 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-[#1877F2] ${
            count === 1 ? "max-h-[500px]" : count === 2 ? "h-[300px]" : count === 3 ? (index === 0 ? "row-span-2 h-[320px]" : "h-[158px]") : index === 4 ? "col-span-2 h-[240px]" : "h-[180px]"
          }`}>
          {media.find(item => item.id === mediaIds[index])?.mediaType === "VIDEO"
            ? <><MediaImage src={`${url}?poster=true`} alt={`Video ${index + 1}`} loading="lazy" draggable={false} className="h-full w-full object-cover" /><span aria-hidden="true" className="absolute inset-0 flex items-center justify-center bg-black/20 text-3xl text-white">▶</span></>
            : <MediaImage src={url} alt={`Ảnh bài viết ${index + 1}`} loading="lazy" draggable={false} className={`w-full rounded-none object-cover ${count === 1 ? "h-auto" : "h-full"}`} />}
          {index === 4 && count > 5 && <span className="absolute inset-0 flex items-center justify-center bg-black/60 text-2xl font-bold text-white">+{count - 5}</span>}
        </button>
      ))}
    </div>
  );
}
