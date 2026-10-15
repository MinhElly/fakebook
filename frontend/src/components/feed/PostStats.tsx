import { Emoji, EmojiStyle } from "emoji-picker-react";

interface Props {
  reactions: number;
  comments: number;
  shares: number;
  topReactions: string[];
  onShowReactors: () => void;
  onShowComments: () => void;
}

const compactCount = new Intl.NumberFormat("en", {
  notation: "compact",
  maximumFractionDigits: 1,
});

const thumbPath = "M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5";
const emojiByReaction: Record<string, string> = {
  haha: "1f606", wow: "1f62e", sad: "1f622", angry: "1f621",
};

function StatIcon({ kind }: { kind: "like" | "comment" | "share" }) {
  return (
    <svg aria-hidden="true" className="size-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      {kind === "like" && <path d={thumbPath} />}
      {kind === "comment" && <path d="M21 11.5a8.5 8.5 0 01-8.5 8.5 9.8 9.8 0 01-4-.9L3 21l1.8-5.1a8.5 8.5 0 1116.2-4.4z" />}
      {kind === "share" && <path d="M14 3l8 7-8 7v-5C8 12 4 15 2 21c0-9 4-14 12-14V3z" />}
    </svg>
  );
}

export default function PostStats({ reactions, comments, shares, topReactions, onShowReactors, onShowComments }: Props) {
  if (reactions === 0 && comments === 0 && shares === 0) return null;

  return (
    <div className="mx-4 flex min-h-10 items-center justify-between gap-3 border-b border-[#E4E6EB] py-2 text-xs text-[#65676B]" data-post-stats>
      <div className="flex items-center gap-4 sm:gap-5">
        {reactions > 0 && (
          <button type="button" aria-label={`${reactions} lượt cảm xúc`} onClick={onShowReactors} className="flex items-center gap-1.5 hover:underline">
            <StatIcon kind="like" /><span>{compactCount.format(reactions)}</span>
          </button>
        )}
        {comments > 0 && (
          <button type="button" aria-label={`${comments} bình luận`} onClick={onShowComments} className="flex items-center gap-1.5 hover:underline">
            <StatIcon kind="comment" /><span>{compactCount.format(comments)}</span>
          </button>
        )}
        {shares > 0 && (
          <span aria-label={`${shares} lượt chia sẻ`} className="flex items-center gap-1.5">
            <StatIcon kind="share" /><span>{compactCount.format(shares)}</span>
          </span>
        )}
      </div>
      {reactions > 0 && (
        <button type="button" aria-label="Xem người đã bày tỏ cảm xúc" onClick={onShowReactors} className="flex shrink-0 items-center pl-1">
          {(topReactions.length ? topReactions : ["like"]).map((type, index) => (
            <span key={type} style={{ zIndex: 3 - index }} className={`flex size-5 items-center justify-center rounded-full border-2 border-white ${index > 0 ? "-ml-1" : ""} ${type === "like" ? "bg-[#1877F2]" : type === "love" ? "bg-[#F33E58]" : "bg-white"}`}>
              {type === "like" ? (
                <svg aria-hidden="true" className="size-3 text-white" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" strokeWidth="1.2"><path d={thumbPath} /></svg>
              ) : type === "love" ? (
                <svg aria-hidden="true" className="size-3 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M12 21l-1.45-1.32C5.4 15 2 11.91 2 8.12A5.45 5.45 0 017.5 2.6 6 6 0 0112 4.7a6 6 0 014.5-2.1A5.45 5.45 0 0122 8.12c0 3.79-3.4 6.88-8.55 11.57z" /></svg>
              ) : emojiByReaction[type] ? <Emoji unified={emojiByReaction[type]} emojiStyle={EmojiStyle.FACEBOOK} size={16} /> : null}
            </span>
          ))}
        </button>
      )}
    </div>
  );
}
