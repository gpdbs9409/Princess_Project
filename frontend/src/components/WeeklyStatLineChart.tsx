import { useState } from "react";
import { GOAL_TYPE_CODES, GOAL_TYPE_LABELS, type GoalTypeCode } from "../api/types";

interface WeeklyStatLineChartProps {
  scores: Partial<Record<string, number>>;
  maxByGoal: Record<string, number>;
}

const WIDTH = 320;
const HEIGHT = 160;
const PAD_X = 16;
const PAD_Y = 16;

export function WeeklyStatLineChart({ scores, maxByGoal }: WeeklyStatLineChartProps) {
  const [hovered, setHovered] = useState<number | null>(null);

  const points = GOAL_TYPE_CODES.map((code, i) => {
    const key = code.toLowerCase();
    const value = scores[key] ?? 0;
    const max = maxByGoal[key] || 1;
    const pct = Math.max(0, Math.min(100, (value / max) * 100));
    const x = PAD_X + (i * (WIDTH - PAD_X * 2)) / (GOAL_TYPE_CODES.length - 1);
    const y = PAD_Y + (HEIGHT - PAD_Y * 2) * (1 - pct / 100);
    return { code, value, pct, x, y };
  });

  const linePath = points.map((p) => `${p.x},${p.y}`).join(" ");
  const baselineY = PAD_Y + (HEIGHT - PAD_Y * 2);

  return (
    <div style={{ position: "relative" }}>
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} width="100%" height={HEIGHT} role="img" aria-label="이번 주 스탯 누적 달성률">
        <line x1={PAD_X} y1={baselineY} x2={WIDTH - PAD_X} y2={baselineY} stroke="var(--border)" strokeWidth={1} />
        <polyline points={linePath} fill="none" stroke="var(--accent)" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />
        {points.map((p, i) => (
          <g key={p.code}>
            <circle
              cx={p.x}
              cy={p.y}
              r={7}
              fill="var(--accent)"
              stroke="var(--surface)"
              strokeWidth={2}
              onMouseEnter={() => setHovered(i)}
              onMouseLeave={() => setHovered((h) => (h === i ? null : h))}
              style={{ cursor: "pointer" }}
            />
            <circle cx={p.x} cy={p.y} r={14} fill="transparent" onMouseEnter={() => setHovered(i)} onMouseLeave={() => setHovered((h) => (h === i ? null : h))} />
          </g>
        ))}
      </svg>
      <div className="row-between" style={{ marginTop: 4 }}>
        {GOAL_TYPE_CODES.map((code) => (
          <span key={code} className="muted" style={{ fontSize: 10.5, flex: 1, textAlign: "center" }}>
            {GOAL_TYPE_LABELS[code]}
          </span>
        ))}
      </div>
      {hovered !== null && (
        <div
          className="week-bar-tooltip"
          style={{
            position: "absolute",
            left: `${(points[hovered].x / WIDTH) * 100}%`,
            top: points[hovered].y - 34,
            transform: "translateX(-50%)",
          }}
        >
          {GOAL_TYPE_LABELS[points[hovered].code as GoalTypeCode]} · {Math.round(points[hovered].value)}점 (
          {Math.round(points[hovered].pct)}%)
        </div>
      )}
    </div>
  );
}
