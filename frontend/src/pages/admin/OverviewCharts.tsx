import { useId, useState } from "react";
import type { AdminOverview } from "@/services/adminService";

const number = (value: number) => value.toLocaleString("vi-VN");
const percent = (value: number, total: number) => total ? Math.round(value / total * 100) : 0;
const dayLabel = (date: string) => date.split("-").reverse().join("/");
const colors = { active: "#1877f2", suspended: "#bd3548", processing: "#a66b08", synced: "#19836e", pending: "#1877f2", retry: "#a66b08" };

export default function OverviewCharts({ data }: { data: AdminOverview }) {
  const prefix = useId();
  const users = [
    { label: "Hoạt động", value: data.user_states.active, color: colors.active },
    { label: "Đã khóa", value: data.user_states.suspended, color: colors.suspended },
    { label: "Đang xử lý", value: data.user_states.processing, color: colors.processing },
  ];
  const synchronization = [
    { label: "Đã đồng bộ", value: data.sync_states.synced, color: colors.synced },
    { label: "Chờ đồng bộ", value: data.sync_states.pending, color: colors.pending },
    { label: "Đang thử lại", value: data.sync_states.retry, color: colors.retry },
  ];
  const totalSync = synchronization.reduce((sum, item) => sum + item.value, 0);
  let offset = 0;
  return <div className="admin-charts">
    <section className="admin-chart-panel" aria-labelledby={`${prefix}-users`}>
      <h2 id={`${prefix}-users`}>Trạng thái người dùng</h2><p className="admin-chart-caption">Cơ cấu tài khoản User hiện tại, không gồm Admin.</p>
      <div className="admin-user-chart">
        <svg viewBox="0 0 200 200" role="img" aria-label={`Trạng thái ${number(data.users)} người dùng: ${users.map(item => `${item.label} ${number(item.value)}`).join(", ")}`}>
          <circle cx="100" cy="100" r="76" fill="none" stroke="#e8edf3" strokeWidth="24" />
          {users.map(item => { const portion = data.users ? item.value / data.users * 100 : 0; const start = offset; offset += portion; return portion > 0 && <circle key={item.label} cx="100" cy="100" r="76" fill="none" stroke={item.color} strokeWidth="24" pathLength="100" strokeDasharray={`${portion} ${100 - portion}`} strokeDashoffset={-start} transform="rotate(-90 100 100)" />; })}
          <text x="100" y="97" textAnchor="middle" className="admin-donut-total">{number(data.users)}</text><text x="100" y="120" textAnchor="middle" className="admin-donut-label">người dùng</text>
        </svg>
        <ul className="admin-chart-legend">{users.map(item => <li key={item.label}><span className="admin-chart-swatch" style={{ background: item.color }} /><span>{item.label}</span><strong>{number(item.value)} <small>({percent(item.value, data.users)}%)</small></strong></li>)}</ul>
      </div>
      {!data.users && <p className="admin-chart-caption">Chưa có tài khoản người dùng.</p>}
    </section>
    <section className="admin-chart-panel" aria-labelledby={`${prefix}-sync`}>
      <h2 id={`${prefix}-sync`}>Đồng bộ tài khoản</h2><p className="admin-chart-caption">Trạng thái của {number(totalSync)} tài khoản Admin và User.</p>
      <div className="admin-sync-summary"><strong>{percent(data.sync_states.synced, totalSync)}%</strong><span>tài khoản đã đồng bộ</span></div>
      <div className="admin-sync-track" role="img" aria-label={synchronization.map(item => `${item.label}: ${number(item.value)}`).join(", ")}>{synchronization.map(item => <span key={item.label} style={{ width: `${totalSync ? item.value / totalSync * 100 : 0}%`, background: item.color }} />)}</div>
      <ul className="admin-chart-legend admin-sync-legend">{synchronization.map(item => <li key={item.label}><span className="admin-chart-swatch" style={{ background: item.color }} /><span>{item.label}</span><strong>{number(item.value)} <small>({percent(item.value, totalSync)}%)</small></strong></li>)}</ul>
      {!totalSync && <p className="admin-chart-caption">Chưa có tài khoản để đồng bộ.</p>}
    </section>
    <ActivityChart data={data} />
  </div>;
}

function ActivityChart({ data }: { data: AdminOverview }) {
  const id = useId();
  const [focused, setFocused] = useState<number | null>(null);
  const maximum = Math.max(0, ...data.activity.map(day => day.views + day.changes));
  const ceiling = Math.max(1, Math.ceil(maximum / 4)) * 4;
  const activity = focused == null ? null : data.activity[focused];
  return <section className="admin-chart-panel admin-activity-panel" aria-labelledby={`${id}-title`}>
    <div className="admin-chart-heading"><div><h2 id={`${id}-title`}>Hoạt động quản trị</h2><p className="admin-chart-caption">{data.days} ngày gần nhất · GMT+7</p></div><div className="admin-chart-series"><span><i className="admin-chart-swatch" style={{ background: colors.active }} />Xem chi tiết/tệp</span><span><i className="admin-chart-swatch" style={{ background: colors.synced }} />Thay đổi quản trị</span></div></div>
    {maximum === 0 && <p className="admin-chart-empty">Chưa có hoạt động quản trị trong khoảng thời gian này.</p>}
    <div className="admin-activity-plot">
      <div className="admin-chart-axis" aria-hidden="true">{[4, 3, 2, 1, 0].map(tick => <span key={tick}>{number(ceiling * tick / 4)}</span>)}</div>
      <div className="admin-chart-bars" style={{ gridTemplateColumns: `repeat(${data.activity.length || data.days}, minmax(0, 1fr))` }}>
        <div className="admin-chart-grid" aria-hidden="true">{[0, 1, 2, 3, 4].map(tick => <span key={tick} />)}</div>
        {data.activity.map((day, index) => <button key={day.date} type="button" className="admin-chart-bar" aria-label={`${dayLabel(day.date)}: Xem chi tiết/tệp ${day.views}, Thay đổi quản trị ${day.changes}`} aria-describedby={`${id}-tooltip`} onFocus={() => setFocused(index)} onBlur={() => setFocused(null)} onMouseEnter={() => setFocused(index)} onMouseLeave={() => setFocused(null)}>
          <span className="admin-bar-stack" aria-hidden="true"><span style={{ height: `${day.changes / ceiling * 100}%`, background: colors.synced }} /><span style={{ height: `${day.views / ceiling * 100}%`, background: colors.active }} /></span>
          {(index === 0 || index === data.activity.length - 1 || index % Math.ceil(data.days / 5) === 0) && <span className="admin-chart-tick" aria-hidden="true">{dayLabel(day.date).slice(0, 5)}</span>}
        </button>)}
      </div>
    </div>
    <p id={`${id}-tooltip`} className="admin-chart-tooltip" role="status">{activity ? `${dayLabel(activity.date)} · Xem chi tiết/tệp: ${number(activity.views)} · Thay đổi quản trị: ${number(activity.changes)}` : "Di chuột hoặc dùng phím Tab trên cột để xem số liệu."}</p>
    <p className="admin-chart-caption">Sự kiện từ dịch vụ khác có thể cập nhật trễ qua Kafka.</p>
    <details className="admin-chart-data"><summary>Xem bảng số liệu hoạt động</summary><div className="admin-table-wrap"><table><caption className="sr-only">Hoạt động quản trị theo ngày, giờ Việt Nam GMT+7</caption><thead><tr><th scope="col">Ngày (GMT+7)</th><th scope="col">Xem chi tiết/tệp</th><th scope="col">Thay đổi quản trị</th><th scope="col">Tổng</th></tr></thead><tbody>{data.activity.map(day => <tr key={day.date}><th scope="row">{dayLabel(day.date)}</th><td>{number(day.views)}</td><td>{number(day.changes)}</td><td>{number(day.views + day.changes)}</td></tr>)}</tbody></table></div></details>
  </section>;
}
