interface StatMeterProps {
  label: string;
  value: number;
  max: number;
}

export function StatMeter({ label, value, max }: StatMeterProps) {
  const pct = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0;
  const roundedValue = Math.round(value);
  const roundedMax = Math.round(max);
  return (
    <div className="stat-meter">
      <div className="stat-meter-head">
        <span>{label}</span>
        <span className="tabular">{roundedValue} / {roundedMax}점</span>
      </div>
      <div
        className={`stat-meter-track${pct === 0 ? " is-empty" : ""}`}
        role="progressbar"
        aria-label={`${label} 달성률`}
        aria-valuemin={0}
        aria-valuemax={roundedMax}
        aria-valuenow={roundedValue}
      >
        <div className="stat-meter-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
