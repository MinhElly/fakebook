export function getNotifIcon(text: string): string {
  if (text.includes("thích")) return "👍";
  if (text.includes("bình luận")) return "💬";
  if (text.includes("kết bạn")) return "👥";
  if (text.includes("chia sẻ")) return "🔄";
  return "🏷️";
}
