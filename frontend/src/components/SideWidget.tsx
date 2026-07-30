import { useEffect, useState } from "react";
import type { ProjectResponse } from "../api/types";
import { GOAL_TYPE_LABELS } from "../api/types";

interface SideWidgetProps {
  project: ProjectResponse | null;
}

// Challenge start line: before it, a countdown to 9/1 00:00:00; from that instant on,
// elapsed time counted up from zero - same H:M:S layout both sides of the line.
const CHALLENGE_START = new Date(2026, 8, 1, 0, 0, 0);

function useClock() {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);
  return now;
}

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

export function SideWidget({ project }: SideWidgetProps) {
  const now = useClock();
  const diffMs = now.getTime() - CHALLENGE_START.getTime();
  const isBeforeStart = diffMs < 0;
  const totalSeconds = Math.floor(Math.abs(diffMs) / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const dDayLabel = isBeforeStart ? "D-DAY" : "D+DAY";

  const topGoals = [...(project?.goals ?? [])].sort((a, b) => b.weightPercent - a.weightPercent).slice(0, 3);

  return (
    <aside className="side-widget">
      <img
        src="/widget/image1.png"
        alt=""
        className="side-widget-photo"
        onError={(e) => {
          e.currentTarget.style.display = "none";
        }}
      />

      <div className="side-widget-card">
        <span className="eyebrow">My Goal</span>
        <p className="side-widget-goal">{project?.goalHuman || "아직 설정되지 않았어요"}</p>
      </div>

      <div className="side-widget-clock">
        <div className="side-widget-clock-seg">
          {isBeforeStart ? "-" : ""}
          {pad(hours)}
        </div>
        <span>:</span>
        <div className="side-widget-clock-seg">{pad(minutes)}</div>
        <span>:</span>
        <div className="side-widget-clock-seg">{pad(seconds)}</div>
        <span className="side-widget-meridiem">{dDayLabel}</span>
      </div>

      <div className="side-widget-card">
        <span className="eyebrow">Priority</span>
        {topGoals.length > 0 ? (
          <ol className="side-widget-priority-list">
            {topGoals.map((g) => (
              <li key={g.id}>
                {GOAL_TYPE_LABELS[g.goalTypeCode]} · {g.weightPercent}%
              </li>
            ))}
          </ol>
        ) : (
          <p className="muted" style={{ fontSize: 12.5 }}>
            습관자본을 설정해보세요
          </p>
        )}
      </div>

      <img
        src="/widget/image2.png"
        alt=""
        className="side-widget-photo"
        onError={(e) => {
          e.currentTarget.style.display = "none";
        }}
      />
    </aside>
  );
}
