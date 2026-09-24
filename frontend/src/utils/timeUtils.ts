export const getTimeAgo = (dateString: string) => {
  if (!dateString) return "Vừa xong";
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffInSeconds < 60) return "Vừa xong";
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} phút`;
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} giờ`;
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)} ngày`;
  
  const options: Intl.DateTimeFormatOptions = { day: "numeric", month: "long" };
  if (date.getFullYear() !== now.getFullYear()) {
    options.year = "numeric";
  }
  return date.toLocaleDateString("vi-VN", options);
};
