import { GOAL_TYPE_CODES, GOAL_TYPE_LABELS } from "../api/types";

interface WeeklyStatLineChartProps {
  scores: Partial<Record<string, number>>;
  goalCodes: string[];
}

const CHART_HEIGHT = 130;
// Every position below is a percentage (0-100) of the chart's own width/height, and every
// element - the SVG line, the markers, the value labels, the category labels - places itself
// using that same percentage, so nothing can drift out of sync with anything else. Circles and
// text are plain HTML overlaid on the SVG rather than SVG shapes, because stretching an SVG
// viewBox non-uniformly to fill the width (needed for the line to reach full width) would
// otherwise squash circles into ellipses and distort the glyphs.
const PAD_X = 8;
const PAD_Y = 20;
const AXIS_TICKS = [100, 70, 50, 30, 0];
const DAYS_IN_WEEK = 7;

function axisTop(tick: number): number {
  return PAD_Y + (100 - PAD_Y * 2) * (1 - tick / 100);
}

export function WeeklyStatLineChart({ scores, goalCodes }: WeeklyStatLineChartProps) {
  const points = goalCodes.map((code, i) => {
    const key = code.toLowerCase();
    const rawTotal = scores[key] ?? 0;
    // Convert the seven-day cumulative total back to its weighted contribution
    // on a 100-point scale. A perfect week therefore ends at 32/24/24/20 = 100.
    const value = rawTotal / DAYS_IN_WEEK;
    const plottedValue = Math.max(0, Math.min(100, value));
    const xPct = goalCodes.length === 1 ? 50 : PAD_X + (i * (100 - PAD_X * 2)) / (goalCodes.length - 1);
    const yPct = PAD_Y + (100 - PAD_Y * 2) * (1 - plottedValue / 100);
    return { code, value, xPct, yPct };
  });

  const linePoints = points.map((p) => `${p.xPct},${p.yPct}`).join(" ");
  const baselineYPct = PAD_Y + (100 - PAD_Y * 2);

  return (
    <div className="weekly-stat-chart-wrap">
      <div className="weekly-stat-chart-axis" aria-hidden="true">
        {AXIS_TICKS.map((tick) => (
          <div key={tick} className="weekly-stat-chart-tick" style={{ top: `${axisTop(tick)}%` }}>
            <span>{tick}</span>
            <i />
          </div>
        ))}
      </div>
      <div className="weekly-stat-chart-plot" style={{ position: "relative", height: CHART_HEIGHT }}>
        <svg
          viewBox="0 0 100 100"
          width="100%"
          height={CHART_HEIGHT}
          preserveAspectRatio="none"
          style={{ position: "absolute", inset: 0 }}
          aria-hidden="true"
        >
          <line x1={PAD_X} y1={baselineYPct} x2={100 - PAD_X} y2={baselineYPct} stroke="var(--border)" strokeWidth={0.3} />
          <polyline points={linePoints} fill="none" stroke="var(--accent)" strokeWidth={0.5} vectorEffect="non-scaling-stroke" />
        </svg>
          <div role="img" aria-label="이번 주 스탯 누적 점수" style={{ position: "absolute", inset: 0 }}>
          {points.map((p) => (
            <div key={p.code} style={{ position: "absolute", left: `${p.xPct}%`, top: `${p.yPct}%`, transform: "translate(-50%, -50%)" }}>
              <span
                className="muted"
                style={{
                  position: "absolute",
                  bottom: 12,
                  left: "50%",
                  transform: "translateX(-50%)",
                  fontSize: 11,
                  fontWeight: 700,
                  color: "var(--text)",
                  whiteSpace: "nowrap",
                }}
              >
                {Number.isInteger(p.value) ? p.value : p.value.toFixed(1)}점
              </span>
              <span
                style={{
                  display: "block",
                  width: 12,
                  height: 12,
                  borderRadius: "50%",
                  background: "var(--accent)",
                  border: "2px solid var(--surface)",
                  boxSizing: "border-box",
                }}
              />
            </div>
          ))}
        </div>
      </div>
      <div style={{ position: "relative", height: 16, marginTop: 4 }}>
        {points.map((p) => (
          <span
            key={p.code}
            className="muted"
            style={{
              position: "absolute",
              left: `${p.xPct}%`,
              transform: "translateX(-50%)",
              fontSize: 10.5,
              whiteSpace: "nowrap",
            }}
          >
            {p.code === "common" ? "공통" : GOAL_TYPE_LABELS[p.code as typeof GOAL_TYPE_CODES[number]]}
          </span>
        ))}
      </div>
    </div>
  );
}
