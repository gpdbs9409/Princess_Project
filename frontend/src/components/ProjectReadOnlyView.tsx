import { GOAL_TYPE_LABELS, type ProjectResponse } from "../api/types";

const MISSION_TYPE_LABELS: Record<string, string> = {
  DAILY: "매일",
  WEEKLY: "매주",
  TOTAL: "전체 기간",
};

export function ProjectReadOnlyView({ project }: { project: ProjectResponse }) {
  return (
    <div className="stack">
      {(project.goalHuman || project.goalAppearance || project.goalEnding) && (
        <div className="card stack" style={{ gap: 6 }}>
          {project.goalHuman && (
            <p>
              <strong>이상적인 나의 모습</strong>: {project.goalHuman}
            </p>
          )}
          {project.goalAppearance && (
            <p>
              <strong>나의 외적 추구미</strong>: {project.goalAppearance}
            </p>
          )}
          {project.goalEnding && (
            <p>
              <strong>목표로 하는 행동양식</strong>: {project.goalEnding}
            </p>
          )}
        </div>
      )}

      {project.goals.map((goal) => (
        <div className="card" key={goal.id}>
          <div className="row-between">
            <strong>{GOAL_TYPE_LABELS[goal.goalTypeCode]}</strong>
            <span className="muted">비중 {goal.weightPercent}%</span>
          </div>
          <div className="stack" style={{ marginTop: 12, gap: 10, paddingLeft: 8, borderLeft: "2px solid var(--border)" }}>
            {goal.stats.map((stat) => (
              <div key={stat.id}>
                <span className="muted" style={{ fontSize: 13.5, fontWeight: 700 }}>
                  {stat.name}
                </span>
                <div className="stack" style={{ marginTop: 6, gap: 6, paddingLeft: 8 }}>
                  {stat.missions.map((mission) => (
                    <div key={mission.id} className="row-between" style={{ fontSize: 13 }}>
                      <span>{mission.name}</span>
                      <span className="muted">
                        {MISSION_TYPE_LABELS[mission.missionType]} · 목표 {mission.targetValue}
                        {mission.unit}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
