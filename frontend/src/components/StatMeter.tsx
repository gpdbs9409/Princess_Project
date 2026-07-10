interface StatMeterProps {
  label: string;
  value: number;
  max: number;
}

export function StatMeter({ label, value, max }: StatMeterProps) {
  const pct = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0;
  return (
    <div className="stat-meter">
      <div className="stat-meter-head">
        <span>{label}</span>
        <span className="tabular">{Math.round(value)}점</span>
      </div>
      <div className="stat-meter-track">
        <div className="stat-meter-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
