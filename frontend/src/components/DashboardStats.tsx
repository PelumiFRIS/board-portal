import type { DashboardStats as DashboardStatsType, MonthlyCount } from "../api/types";

function formatPercent(rate: number): string {
  return `${Math.round(rate * 100)}%`;
}

interface StatCardProps {
  label: string;
  value: number;
  breakdown: string;
  overdue?: number;
  overdueLabel?: string;
}

function StatCard({ label, value, breakdown, overdue, overdueLabel }: StatCardProps) {
  return (
    <div className="stat-card">
      <span className="stat-card-label">{label}</span>
      <span className="stat-card-value">{value}</span>
      <span className="stat-card-breakdown">{breakdown}</span>
      {overdue !== undefined && overdue > 0 && (
        <span className="badge badge-cancelled stat-card-overdue">
          {overdue} {overdueLabel ?? "overdue"}
        </span>
      )}
    </div>
  );
}

function CadenceChart({ cadence }: { cadence: MonthlyCount[] }) {
  const max = Math.max(1, ...cadence.map((m) => m.count));
  const labelSpace = 16;
  const barAreaHeight = 96;
  const chartHeight = barAreaHeight + labelSpace;

  return (
    <svg
      className="cadence-chart-svg"
      viewBox={`0 0 ${cadence.length * 48} ${chartHeight + 24}`}
      preserveAspectRatio="xMidYMid meet"
      role="img"
      aria-label="Meetings scheduled per month over the last 6 months"
    >
      {cadence.map((m, i) => {
        const barHeight = m.count === 0 ? 0 : (m.count / max) * barAreaHeight;
        const x = i * 48;
        return (
          <g key={m.month}>
            <rect
              x={x + 10}
              y={chartHeight - barHeight}
              width={28}
              height={barHeight}
              rx={4}
              fill="var(--primary)"
              opacity={m.count === 0 ? 0.15 : 0.85}
            />
            {m.count === 0 && (
              <rect x={x + 10} y={chartHeight - 3} width={28} height={3} rx={1.5} fill="var(--border)" />
            )}
            {m.count > 0 && (
              <text x={x + 24} y={chartHeight - barHeight - 6} textAnchor="middle" className="cadence-chart-count">
                {m.count}
              </text>
            )}
            <text x={x + 24} y={chartHeight + 18} textAnchor="middle" className="cadence-chart-label">
              {m.month.split(" ")[0]}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

function RateBar({ label, rate }: { label: string; rate: number }) {
  const pct = Math.max(0, Math.min(1, rate)) * 100;
  return (
    <div className="rate-bar">
      <div className="rate-bar-header">
        <span>{label}</span>
        <span>{formatPercent(rate)}</span>
      </div>
      <div className="rate-bar-track">
        <div className="rate-bar-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export function DashboardStats({ stats }: { stats: DashboardStatsType }) {
  const { meetings, resolutions, actionItems, compliance } = stats;

  return (
    <section className="dashboard-section">
      <h2>Governance overview</h2>
      <div className="stat-card-grid">
        <StatCard
          label="Meetings"
          value={meetings.total}
          breakdown={`${meetings.scheduled} scheduled · ${meetings.completed} completed · ${meetings.cancelled} cancelled`}
        />
        <StatCard
          label="Resolutions"
          value={resolutions.total}
          breakdown={`${resolutions.open} open · ${resolutions.closed} closed`}
        />
        <StatCard
          label="Action items"
          value={actionItems.total}
          breakdown={`${actionItems.open} open · ${actionItems.done} done`}
          overdue={actionItems.overdue}
        />
        <StatCard
          label="Compliance filings"
          value={compliance.total}
          breakdown={`${compliance.submitted} submitted · ${compliance.pending} pending`}
          overdue={compliance.overdue}
        />
      </div>

      <div className="stats-detail-grid">
        <div className="cadence-chart">
          <h3>Meeting cadence (last 6 months)</h3>
          <CadenceChart cadence={meetings.cadence} />
        </div>
        <div className="rate-bar-group">
          <h3>Rates</h3>
          <RateBar label="Resolution pass rate" rate={resolutions.passRate} />
          <RateBar label="Compliance rate" rate={compliance.complianceRate} />
        </div>
      </div>
    </section>
  );
}
