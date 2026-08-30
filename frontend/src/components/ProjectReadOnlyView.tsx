import { GOAL_TYPE_IMAGE, GOAL_TYPE_LABELS, type ProjectResponse } from "../api/types";

export function ProjectReadOnlyView({ project }: { project: ProjectResponse }) {
  return (
    <div className="stack">
      {(project.goalHuman || project.goalAppearance || project.goalEnding) && (
        <div className="card stack" style={{ gap: 6 }}>
          {project.goalHuman && (
            <p>
              <strong>내가 되고 싶은 인간상</strong>: {project.goalHuman}
            </p>
          )}
          {project.goalAppearance && (
            <p>
              <strong>외적 페르소나</strong>: {project.goalAppearance}
            </p>
          )}
          {project.goalEnding && (
            <p>
              <strong>내가 살고 싶은 삶</strong>: {project.goalEnding}
            </p>
          )}
        </div>
      )}

      {(project.commonReadingBookTitle || project.commonStudyYoutubeTopic) && (
        <div className="card stack" style={{ gap: 10 }}>
          <strong>공통자본</strong>
          {project.commonReadingBookTitle && (
            <div><span className="muted">독서</span><div>{project.commonReadingBookTitle} · {project.commonReadingTotalPages}쪽</div></div>
          )}
          {project.commonStudyYoutubeTopic && (
            <div><span className="muted">공부 YouTube 주제</span><div>{project.commonStudyYoutubeTopic}</div></div>
          )}
        </div>
      )}

      {project.goals.map((goal) => (
        <div className="card habitus-readonly-card" key={goal.id}>
          <img
            src={GOAL_TYPE_IMAGE[goal.goalTypeCode]}
            alt=""
            className="habitus-readonly-image"
            aria-hidden="true"
          />
          <div className="habitus-readonly-content">
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
                        목표 {mission.targetValue}
                        {mission.unit}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
