interface Props {
  title: string;
  description: string;
  compact?: boolean;
}

export default function FeatureEmptyState({ title, description, compact = false }: Props) {
  return (
    <div
      className={`flex flex-col items-center justify-center text-center ${
        compact ? "px-5 py-8" : "min-h-48 px-6 py-10"
      }`}
      role="status"
    >
      <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-[#E7F3FF] text-[#1877F2]">
        <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 8v4m0 4h.01M10.3 3.7 2.6 17a2 2 0 0 0 1.73 3h15.34a2 2 0 0 0 1.73-3L13.7 3.7a2 2 0 0 0-3.4 0Z" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>
      <p className="font-semibold text-[#1C1E21]">{title}</p>
      <p className="mt-1 max-w-[34rem] text-sm leading-5 text-[#65676B]">{description}</p>
    </div>
  );
}
