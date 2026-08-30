import { useState } from "react";

interface DayPoint {
  date: string;
  label: string;
  value: number;
  /** achievement rate against that day's own max possible score, 0-1 */
  progress: number;
  refundCredit: number;
  isToday: boolean;
}

// 막대 높이는 그날의 달성률(0~100%)이라 눈금도 %로 둔다. 눈금이 없으면 막대끼리 비교만
// 될 뿐 "오늘이 대략 몇 %인지"를 읽을 수 없어서, 자주 보는 구간 위주로 5개만 그린다.
const AXIS_TICKS = [100, 70, 50, 30, 0];

export function WeeklyBarChart({ days }: { days: DayPoint[] }) {
  const [hovered, setHovered] = useState<number | null>(null);

  return (
    <div className="week-chart-wrap">
      <div className="week-chart-axis" aria-hidden="true">
        {AXIS_TICKS.map((tick) => (
          <div key={tick} className="week-chart-tick" style={{ bottom: `${tick}%` }}>
            <span className="week-chart-tick-label">{tick}</span>
            <span className="week-chart-tick-line" />
          </div>
        ))}
      </div>
      <div className="week-chart">
        {days.map((day, i) => {
          const heightPct = day.value > 0 ? Math.max(2, Math.min(100, day.progress * 100)) : 0;
          return (
            <div
              key={day.date}
              className="week-bar-col"
              onMouseEnter={() => setHovered(i)}
              onMouseLeave={() => setHovered((h) => (h === i ? null : h))}
            >
              {hovered === i && (
                <div className="week-bar-tooltip">
                  {day.label} · {Math.round(day.value)}점
                </div>
              )}
              <div
                className={`week-bar ${day.value > 0 ? "has-value" : ""} ${day.isToday ? "is-today" : ""}`}
                style={{ height: `${heightPct}%` }}
              />
            </div>
          );
        })}
      </div>
      <div className="week-bar-labels">
        {days.map((day) => (
          <span key={day.date} className={`week-bar-label ${day.isToday ? "is-today" : ""}`}>
            {day.label}
          </span>
        ))}
      </div>
      <div className="week-refund-hearts" aria-label="요일별 환급 조건 달성 여부">
        {days.map((day) => {
          const full = day.refundCredit >= 1;
          const half = day.refundCredit > 0 && day.refundCredit < 1;
          const label = full ? "모든 미션 수행" : half ? "일부 미션 수행" : "수행 기록 없음";
          return (
            <span key={day.date} className="week-refund-heart-slot" title={`${day.label}요일 · ${label}`}>
              {(full || half) && (
                <span className={`week-refund-heart ${half ? "is-half" : ""}`} aria-label={label}>
                  <img src="/cursor/cursor.png" alt="" />
                </span>
              )}
            </span>
          );
        })}
      </div>
      <div className="week-refund-legend">
        <span><span className="week-refund-heart"><img src="/cursor/cursor.png" alt="" /></span> 전체 수행</span>
        <span><span className="week-refund-heart is-half"><img src="/cursor/cursor.png" alt="" /></span> 일부 수행</span>
      </div>
    </div>
  );
}
