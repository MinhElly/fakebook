import { useState } from "react";
import { getMediaUrl } from "@/services/mediaService";

interface MediaGridProps {
  mediaIds?: string[];
  images?: (string | null)[];
}

export default function MediaGrid({ mediaIds = [], images = [] }: MediaGridProps) {
  const [selectedMedia, setSelectedMedia] = useState<string | null>(null);

  // Combine mediaIds and image URLs
  const mediaList: string[] = [];

  if (mediaIds && mediaIds.length > 0) {
    mediaIds.forEach(id => {
      if (id) mediaList.push(getMediaUrl(id));
    });
  } else if (images && images.length > 0) {
    images.forEach(img => {
      if (img) mediaList.push(getMediaUrl(img));
    });
  }

  if (mediaList.length === 0) return null;

  const count = mediaList.length;

  return (
    <>
      <div className="mt-3 rounded-xl overflow-hidden border border-[#E4E6EB] bg-black/5">
        {count === 1 && (
          <div
            className="w-full max-h-[500px] overflow-hidden cursor-pointer flex items-center justify-center bg-black/10"
            onClick={() => setSelectedMedia(mediaList[0])}
          >
            <img
              src={mediaList[0]}
              alt="Post attachment"
              className="w-full h-auto object-cover hover:scale-[1.01] transition-transform duration-200"
              onError={e => {
                (e.target as HTMLImageElement).style.display = "none";
              }}
            />
          </div>
        )}

        {count === 2 && (
          <div className="grid grid-cols-2 gap-1 max-h-[400px]">
            {mediaList.slice(0, 2).map((url, index) => (
              <div
                key={index}
                className="h-[300px] overflow-hidden cursor-pointer relative bg-black/10"
                onClick={() => setSelectedMedia(url)}
              >
                <img
                  src={url}
                  alt={`Attachment ${index + 1}`}
                  className="w-full h-full object-cover hover:scale-105 transition-transform duration-200"
                />
              </div>
            ))}
          </div>
        )}

        {count === 3 && (
          <div className="grid grid-cols-3 gap-1 max-h-[400px]">
            <div
              className="col-span-2 h-[320px] overflow-hidden cursor-pointer relative bg-black/10"
              onClick={() => setSelectedMedia(mediaList[0])}
            >
              <img
                src={mediaList[0]}
                alt="Attachment 1"
                className="w-full h-full object-cover hover:scale-105 transition-transform duration-200"
              />
            </div>
            <div className="col-span-1 flex flex-col gap-1 h-[320px]">
              {mediaList.slice(1, 3).map((url, index) => (
                <div
                  key={index}
                  className="h-1/2 overflow-hidden cursor-pointer relative bg-black/10"
                  onClick={() => setSelectedMedia(url)}
                >
                  <img
                    src={url}
                    alt={`Attachment ${index + 2}`}
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-200"
                  />
                </div>
              ))}
            </div>
          </div>
        )}

        {count >= 4 && (
          <div className="grid grid-cols-2 gap-1 max-h-[420px]">
            {mediaList.slice(0, 3).map((url, index) => (
              <div
                key={index}
                className="h-[180px] overflow-hidden cursor-pointer relative bg-black/10"
                onClick={() => setSelectedMedia(url)}
              >
                <img
                  src={url}
                  alt={`Attachment ${index + 1}`}
                  className="w-full h-full object-cover hover:scale-105 transition-transform duration-200"
                />
              </div>
            ))}
            <div
              className="h-[180px] overflow-hidden cursor-pointer relative bg-black/10"
              onClick={() => setSelectedMedia(mediaList[3])}
            >
              <img
                src={mediaList[3]}
                alt="Attachment 4"
                className="w-full h-full object-cover"
              />
              {count > 4 && (
                <div className="absolute inset-0 bg-black/60 flex items-center justify-center text-white font-bold text-2xl hover:bg-black/70 transition-colors">
                  +{count - 4}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Lightbox Modal */}
      {selectedMedia && (
        <div
          className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4 backdrop-blur-sm"
          onClick={() => setSelectedMedia(null)}
        >
          <div className="relative max-w-5xl max-h-[90vh] flex items-center justify-center">
            <button
              className="absolute -top-10 right-0 text-white text-3xl font-bold hover:text-gray-300"
              onClick={() => setSelectedMedia(null)}
            >
              ✕
            </button>
            <img
              src={selectedMedia}
              alt="Enlarged preview"
              className="max-w-full max-h-[85vh] object-contain rounded-lg shadow-2xl"
              onClick={e => e.stopPropagation()}
            />
          </div>
        </div>
      )}
    </>
  );
}
