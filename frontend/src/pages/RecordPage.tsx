import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { generateAiFeedback, getActiveProject, getDailySummary } from "../api/endpoints";
import { GOAL_TYPE_LABELS, type DailySummaryResponse, type ProjectResponse } from "../api/types";
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
  const [feedbackLoading, setFeedbackLoading] = useState(false);
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

  const handleGenerateFeedback = async () => {
    setFeedbackLoading(true);
    try {
      const feedback = await generateAiFeedback(date);
      setSummary((s) => (s ? { ...s, aiFeedback: feedback } : s));
    } catch {
      setError("AI 피드백 생성에 실패했습니다.");
    } finally {
      setFeedbackLoading(false);
    }
  };

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

      {summary && (
        <div className="card">
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

      {!loading && hasNoMissions && (
        <div className="card">
          <p className="muted">아직 선택한 미션이 없어요.</p>
          <Link to="/stat-focus" className="link">
            습관자본·미션 설정하러 가기 →
          </Link>
        </div>
      )}

      {missions.length > 0 && (
        <div className="habit-tracker-card" style={{ marginTop: 16 }}>
          <div className="row-between" style={{ marginBottom: 4 }}>
            <span className="badge good">✓ Habit Tracker</span>
          </div>
          {missions.map((mission) => (
            <MissionCard
              key={mission.userMissionId}
              mission={mission}
              date={date}
              completed={summary?.completedMissions.includes(mission.name) ?? false}
              onSaved={load}
            />
          ))}
        </div>
      )}

      {missions.length > 0 && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="row-between">
            <strong>오늘 완료</strong>
            <button className="ghost" onClick={handleGenerateFeedback} disabled={feedbackLoading}>
              {feedbackLoading ? "생성 중..." : "AI 피드백 받기"}
            </button>
          </div>
          {summary?.aiFeedback && (
            <div className="stack" style={{ marginTop: 12, gap: 8 }}>
              <p>{summary.aiFeedback.summary}</p>
              <p className="muted">👏 {summary.aiFeedback.praise}</p>
              <p className="muted">🌱 {summary.aiFeedback.improvement}</p>
              <p className="muted">➡️ {summary.aiFeedback.tomorrow}</p>
              <p className="muted">💌 {summary.aiFeedback.cheer}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
