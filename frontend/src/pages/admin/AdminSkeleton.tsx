import { LoadingRegion, Skeleton } from "@/components/ui/Skeleton";
import "./admin.css";

export default function AdminSkeleton({ kind = "table", columns = 6 }: { kind?: "overview" | "table" | "detail"; columns?: number }) {
  return <LoadingRegion label={kind === "detail" ? "Đang tải chi tiết" : "Đang tải dữ liệu quản trị"} className="admin-skeleton">
    {kind === "overview" ? <><div className="admin-metrics">{Array.from({ length: 4 }, (_, index) => <div key={index}><Skeleton className="h-3 w-28 max-w-full" /><Skeleton className="mt-5 h-8 w-16" /></div>)}</div><div className="admin-charts">{[0, 1, 2].map(index => <div key={index} className={`admin-chart-panel ${index === 2 ? "admin-activity-panel" : ""}`}><Skeleton className="mb-3 h-5 w-44 max-w-full" /><Skeleton className="mb-6 h-3 w-64 max-w-full" /><Skeleton className={index === 0 ? "mx-auto h-44 w-44 rounded-full" : "h-44 w-full"} /></div>)}</div></>
      : kind === "detail" ? <div className="admin-detail-skeleton">{Array.from({ length: 6 }, (_, index) => <div key={index}><Skeleton className="h-3 w-28 max-w-full" /><Skeleton className={`h-4 ${index === 2 ? "w-full" : "w-2/3"}`} /></div>)}</div>
      : <><Skeleton className="mb-4 h-3 w-24" /><div className="admin-table-wrap"><table><thead><tr>{Array.from({ length: columns }, (_, index) => <th key={index}><Skeleton className="h-3 w-24" /></th>)}</tr></thead>
        <tbody>{Array.from({ length: 6 }, (_, row) => <tr key={row}>{Array.from({ length: columns }, (_, column) => <td key={column}><Skeleton className={`h-4 ${column % 2 ? "w-32" : "w-24"}`} /></td>)}</tr>)}</tbody></table></div><div className="admin-pagination"><Skeleton className="h-9 w-20" /><Skeleton className="h-3 w-16" /><Skeleton className="h-9 w-20" /></div></>}
  </LoadingRegion>;
}

export function AdminShellSkeleton() {
  return <div className="admin-shell"><aside className="admin-sidebar" aria-hidden="true"><Skeleton className="mb-10 h-10 w-36 max-w-full" /><div className="space-y-5">{Array.from({ length: 6 }, (_, index) => <Skeleton key={index} className="h-5 w-32 max-w-full" />)}</div></aside>
    <div className="admin-workspace"><div className="admin-topbar" aria-hidden="true"><Skeleton className="h-8 w-40" /></div><div className="admin-content"><div aria-hidden="true"><Skeleton className="mb-3 h-8 w-48 max-w-full" /><Skeleton className="mb-8 h-3 w-72 max-w-full" /></div><AdminSkeleton kind="overview" /></div></div>
  </div>;
}
