import { useState } from "react";
import type { EndingWeek, GoalTypeCode } from "../api/types";
import { GOAL_TYPE_LABELS } from "../api/types";
import { CAPITAL_COLORS } from "../lib/capitalScore";

interface EndingStatChartProps {
  seriesKeys: string[];
  weeks: EndingWeek[];
}

// 좌표계: viewBox 600×260 (가로세로 같은 비율로 스케일되므로 글자·점이 찌그러지지 않는다).
const VB_W = 600;
const VB_H = 260;
const PAD_L = 44;
const PAD_R = 16;
const PAD_T = 16;
const PAD_B = 34;

function niceMax(value: number): number {
  if (value <= 0) return 100;
  const exp = Math.pow(10, Math.floor(Math.log10(value)));
  for (const m of [1, 2, 2.5, 5, 10]) {
    if (m * exp >= value) return m * exp;
  }
  return 10 * exp;
}

function label(key: string): string {
  return key === "common" ? "공통 (독서·공부)" : GOAL_TYPE_LABELS[key as GoalTypeCode] ?? key;
}

/**
 * "나의 한달동안의 기록" - 1~4주차 스탯 누적 꺾은선 (대시보드 '이번 주 스탯 누적'과 같은 점수·색 체계).
 * 계열 = 선택 3자본 + 공통(독서·공부). 범례를 누르면 해당 계열이 강조된다.
 */
export function EndingStatChart({ seriesKeys, weeks }: EndingStatChartProps) {
  const [focused, setFocused] = useState<string | null>(null);

  const series = seriesKeys.map((key) => {
    const scoreKey = key.toLowerCase();
    let running = 0;
    const values = weeks.map((week) => {
      running += Number(week.scores[scoreKey] ?? 0);
      return running;
    });
    return { key, scoreKey, values, color: CAPITAL_COLORS[scoreKey] ?? "var(--accent)" };
  });

  const hasAny = series.some((s) => s.values.some((v) => v > 0));
  if (!hasAny || weeks.length === 0) {
    return <p className="muted ending-chart-empty">기록된 스탯이 없습니다</p>;
  }

  const maxValue = niceMax(Math.max(...series.flatMap((s) => s.values)));
  const ticks = [0, 0.25, 0.5, 0.75, 1].map((r) => Math.round(maxValue * r));
  const plotW = VB_W - PAD_L - PAD_R;
  const plotH = VB_H - PAD_T - PAD_B;
  const x = (i: number) => (weeks.length === 1 ? PAD_L + plotW / 2 : PAD_L + (plotW * i) / (weeks.length - 1));
  const y = (v: number) => PAD_T + plotH * (1 - v / maxValue);

  return (
    <div className="ending-chart">
      <svg viewBox={`0 0 ${VB_W} ${VB_H}`} className="ending-chart-svg" role="img" aria-label="주차별 스탯 누적 점수">
        {ticks.map((tick) => (
          <g key={tick}>
            <line x1={PAD_L} x2={VB_W - PAD_R} y1={y(tick)} y2={y(tick)} stroke="var(--border)" strokeWidth={0.6} strokeDasharray={tick === 0 ? undefined : "3 4"} />
            <text x={PAD_L - 8} y={y(tick) + 4} textAnchor="end" fontSize={11} fill="var(--text-muted)">
              {tick}
            </text>
          </g>
        ))}
        {weeks.map((week, i) => (
          <text key={week.week} x={x(i)} y={VB_H - 10} textAnchor="middle" fontSize={12} fill="var(--text-muted)">
            {week.week}주차
          </text>
        ))}
        {series.map((s) => {
          const dim = focused !== null && focused !== s.key;
          const strong = focused === s.key;
          return (
            <g key={s.key} opacity={dim ? 0.18 : 1} style={{ transition: "opacity 0.2s" }}>
              <polyline
                points={s.values.map((v, i) => `${x(i)},${y(v)}`).join(" ")}
                fill="none"
                stroke={s.color}
                strokeWidth={strong ? 3.5 : 2.2}
                strokeLinejoin="round"
                strokeLinecap="round"
              />
              {s.values.map((v, i) => (
                <g key={i}>
                  <circle cx={x(i)} cy={y(v)} r={strong ? 5 : 4} fill={s.color} stroke="var(--surface)" strokeWidth={1.5}>
                    <title>{`${label(s.key)} · ${weeks[i].week}주차 누적 ${Math.round(v)}점`}</title>
                  </circle>
                  {strong && (
                    <text x={x(i)} y={y(v) - 10} textAnchor="middle" fontSize={11} fontWeight={700} fill="var(--text)">
                      {Math.round(v)}
                    </text>
                  )}
                </g>
              ))}
            </g>
          );
        })}
      </svg>
      <div className="ending-chart-legend">
        {series.map((s) => (
          <button
            key={s.key}
            type="button"
            className={`ending-chart-legend-item ${focused === s.key ? "is-active" : ""}`}
            aria-pressed={focused === s.key}
            onClick={() => setFocused((f) => (f === s.key ? null : s.key))}
          >
            <span className="ending-chart-swatch" style={{ background: s.color }} />
            {label(s.key)}
          </button>
        ))}
      </div>
    </div>
  );
}
