import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getActiveProject, getDailySummary } from "../api/endpoints";
import { GOAL_TYPE_LABELS, type DailySummaryResponse, type ProjectResponse } from "../api/types";
import { CommonTasksCard } from "../components/CommonTasksCard";
import { MissionCard, type FlatMission } from "../components/MissionCard";
import { SideWidget } from "../components/SideWidget";

function todayIso(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

export function RecordPage() {
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [missions, setMissions] = useState<FlatMission[]>([]);
  const [summary, setSummary] = useState<DailySummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [hasNoMissions, setHasNoMissions] = useState(false);
  const date = todayIso();

  const load = useCallback(async () => {
    try {
      const [projectData, dailySummary] = await Promise.all([getActiveProject(), getDailySummary(date)]);
      const flattened: FlatMission[] = projectData.goals.flatMap((goal) =>
        goal.stats.flatMap((stat) =>
          stat.missions.map((mission) => ({
            userMissionId: mission.id,
            name: mission.name,
            targetValue: mission.targetValue,
            unit: mission.unit,
            goalLabel: GOAL_TYPE_LABELS[goal.goalTypeCode],
            goalTypeCode: goal.goalTypeCode,
          }))
        )
      );
      setProject(projectData);
      setMissions(flattened);
      setHasNoMissions(flattened.length === 0);
      setSummary(dailySummary);
    } catch {
      setError("데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [date]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="container">
      <SideWidget project={project} />

      <div className="row-between" style={{ marginBottom: 20 }}>
        <div>
          <span className="eyebrow">{date}</span>
          <h1 style={{ fontSize: 26 }}>오늘의 기록</h1>
        </div>
      </div>

      {error && <div className="error-banner" style={{ marginBottom: 16 }}>{error}</div>}

      {/* 오늘 총점 - 페이지에서 가장 먼저 눈에 들어와야 하는 요약이라 맨 위로 옮겼다
          (2026-08-21 요청: 오늘총점은 가장상단에 와야하고 그 아래 공통과제 2종 및 타
          습관기록도 같이). */}
      {summary && (
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="row-between">
            <strong>오늘 총점</strong>
            <span className="tabular" style={{ fontSize: 20, fontWeight: 700 }}>
              {Math.round(summary.totalScore)}점
            </span>
          </div>
          <div className="stat-meter-track" style={{ marginTop: 10 }}>
            <div
              className="stat-meter-fill"
              style={{ width: `${Math.min(100, summary.progress * 100)}%` }}
            />
          </div>
          <p className="muted" style={{ marginTop: 8 }}>
            달성률 {Math.round(summary.progress * 100)}% · 완료{" "}
            {summary.completedMissions.length > 0 ? summary.completedMissions.join(", ") : "없음"}
          </p>
        </div>
      )}

      {loading && <p className="muted">불러오는 중...</p>}

      {/* 오늘 총점 아래로, 독서/공부 공통 과제와 다른 습관 기록을 한 목록에 이어서 보여준다
          (2026-08-21 요청). 독서/공부는 어떤 아비투스를 골랐든 전원 필수라 hasNoMissions와
          무관하게 항상 먼저 렌더링되고, 그 아래로 선택한 습관 미션 카드가 이어진다.
          주간 회고는 여기 없다 - 주 1회만 쓰면 되는 과제라 상단 메뉴의 "주간 회고"
          (/weekly-retrospective)로 분리했다 (2026-08-21 요청). */}
      <div className="stack" style={{ marginBottom: 16, gap: 12 }}>
        <span className="badge good" style={{ alignSelf: "flex-start" }}>
          오늘의 기록
        </span>
        <CommonTasksCard />
        {missions.map((mission) => (
          <MissionCard
            key={mission.userMissionId}
            mission={mission}
            date={date}
            completed={summary?.completedMissions.includes(mission.name) ?? false}
            record={summary?.todayRecords[mission.userMissionId]}
            onSaved={load}
          />
        ))}
      </div>

      <p className="muted" style={{ marginTop: -4, marginBottom: 16, fontSize: 12.5 }}>
        주간 회고는 상단 메뉴의{" "}
        <Link to="/weekly-retrospective" className="link">
          주간 회고
        </Link>
        에서 주 1회 작성해요.
      </p>

      {!loading && hasNoMissions && (
        <div className="card">
          <p className="muted">아직 선택한 미션이 없어요.</p>
          <Link to="/stat-focus" className="link">
            아비투스·미션 설정하러 가기 →
          </Link>
        </div>
      )}

    </div>
  );
}
