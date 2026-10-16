export type PresenceStatus = "online" | "offline" | "unknown";

interface Props {
    status?: PresenceStatus;
}

export default function PresenceBadge({
                                          status = "unknown",
                                      }: Props) {
    if (status === "unknown") return null;

    const online = status === "online";
    const label = online ? "Đang hoạt động" : "Ngoại tuyến";

    return (
        <span
            role="img"
            aria-label={label}
            title={label}
            className={`absolute bottom-0 right-0 h-2.5 w-2.5
        rounded-full border-2 border-white ${
                online ? "bg-green-500" : "bg-gray-400"
            }`}
        />
    );
}