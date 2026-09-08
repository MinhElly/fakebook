import { useEffect } from "react";

export interface ToastProps {
  message: string;
  type: "success" | "error" | "info";
  onClose: () => void;
  duration?: number;
}

export default function Toast({ message, type, onClose, duration = 3000 }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, duration);
    return () => clearTimeout(timer);
  }, [onClose, duration]);

  const isSuccess = type === "success";

  return (
    <div className="fixed top-5 right-5 z-[200] flex items-center gap-3 px-4 py-3 rounded-xl shadow-xl bg-white border border-[#E4E6EB] transition-all animate-bounce-in">
      <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${isSuccess ? "bg-green-100 text-green-600" : "bg-red-100 text-red-600"}`}>
        {isSuccess ? (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
          </svg>
        ) : (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M6 18L18 6M6 6l12 12" />
          </svg>
        )}
      </div>

      <div className="flex-1 text-sm font-semibold text-[#1C1E21]">
        {message}
      </div>

      <button
        onClick={onClose}
        className="text-[#65676B] hover:text-[#1C1E21] p-1 rounded-lg hover:bg-[#F0F2F5] transition-colors"
      >
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
        </svg>
      </button>
    </div>
  );
}
