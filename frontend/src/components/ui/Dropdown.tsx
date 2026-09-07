interface DropdownProps {
  children: React.ReactNode;
  className?: string;
}

export default function Dropdown({ children, className = "" }: DropdownProps) {
  return (
    <div className={`absolute top-[calc(100%+8px)] right-0 bg-white rounded-xl shadow-2xl border border-[#E4E6EB] z-50 overflow-hidden ${className}`}>
      {children}
    </div>
  );
}
