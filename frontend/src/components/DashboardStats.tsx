import type { ReactNode } from "react";
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
  accent: "primary" | "gold" | "info" | "success";
  icon: ReactNode;
}

function StatCard({ label, value, breakdown, overdue, overdueLabel, accent, icon }: StatCardProps) {
  return (
    <div className={`stat-card stat-card-${accent}`}>
      <span className="stat-card-icon">{icon}</span>
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

function CalendarIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18.75zm0-7.5h18"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function GavelIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ChecklistIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9 6.75h10.5M9 12h10.5M9 17.25h10.5M4.5 6.75h.008v.008H4.5V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM4.5 12h.008v.008H4.5V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.008v.008H4.5v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ShieldIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9 12.75L11.25 15 15 9.75m6-3.75c0 5.592-3.824 9.75-9 11.25C6.824 15.75 3 11.592 3 6c1.6-.6 3.6-1.2 6-1.2s4.4.6 6 1.2z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
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
          accent="primary"
          icon={<CalendarIcon />}
        />
        <StatCard
          label="Resolutions"
          value={resolutions.total}
          breakdown={`${resolutions.open} open · ${resolutions.closed} closed`}
          accent="gold"
          icon={<GavelIcon />}
        />
        <StatCard
          label="Action items"
          value={actionItems.total}
          breakdown={`${actionItems.open} open · ${actionItems.done} done`}
          overdue={actionItems.overdue}
          accent="info"
          icon={<ChecklistIcon />}
        />
        <StatCard
          label="Compliance filings"
          value={compliance.total}
          breakdown={`${compliance.submitted} submitted · ${compliance.pending} pending`}
          overdue={compliance.overdue}
          accent="success"
          icon={<ShieldIcon />}
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
