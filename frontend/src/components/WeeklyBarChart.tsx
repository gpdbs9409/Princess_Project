import { useState } from "react";

interface DayPoint {
  date: string;
  label: string;
  value: number;
  isToday: boolean;
}

export function WeeklyBarChart({ days }: { days: DayPoint[] }) {
  const [hovered, setHovered] = useState<number | null>(null);
  const max = Math.max(1, ...days.map((d) => d.value));

  return (
    <div>
      <div className="week-chart">
        {days.map((day, i) => {
          const heightPct = Math.max(2, (day.value / max) * 100);
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
    </div>
  );
}
