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
            <div><span className="muted">공부 주제</span><div>{project.commonStudyYoutubeTopic}</div></div>
          )}
        </div>
      )}

      {project.goals.map((goal) => (
        <div className="card habitus-readonly-card" key={goal.id}>
          <div className="habitus-readonly-header">
            <img
              src={GOAL_TYPE_IMAGE[goal.goalTypeCode]}
              alt=""
              className="habitus-readonly-image"
              aria-hidden="true"
            />
            <div>
              <strong className="habitus-readonly-title">{GOAL_TYPE_LABELS[goal.goalTypeCode]}</strong>
              <div className="muted habitus-readonly-weight">비중 {goal.weightPercent}%</div>
            </div>
          </div>
          <div className="habitus-readonly-missions">
            {goal.stats.map((stat) => (
              <div className="habitus-readonly-stat" key={stat.id}>
                <span className="muted habitus-readonly-stat-name">{stat.name}</span>
                <div className="habitus-readonly-mission-list">
                  {stat.missions.map((mission) => (
                    <div key={mission.id} className="habitus-readonly-mission">
                      <strong>{mission.name}</strong>
                      <span className="muted">목표 {mission.targetValue}{mission.unit}</span>
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
