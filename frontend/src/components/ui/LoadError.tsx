export default function LoadError({
  message = "Không tải được dữ liệu.",
  onRetry,
}: {
  message?: string
  onRetry: () => void
}) {
  return (
    <p
      role="alert"
      className="my-3 rounded-lg bg-white p-3 text-sm text-[#B42318]"
    >
      {message}{" "}
      <button
        type="button"
        onClick={onRetry}
        className="font-semibold text-[#1877F2] underline focus-visible:outline-2"
      >
        Thử lại
      </button>
    </p>
  )
}
