import { useCallback, useEffect, useState } from "react";
import { generateAiFeedback, getDailySummary, getMissions } from "../api/endpoints";
import type { DailySummaryResponse, MissionResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { MissionCard } from "../components/MissionCard";

function todayIso(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

export function RecordPage() {
  const { user } = useAuth();
  const [missions, setMissions] = useState<MissionResponse[]>([]);
  const [summary, setSummary] = useState<DailySummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [feedbackLoading, setFeedbackLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const date = todayIso();

  const load = useCallback(async () => {
    if (!user) return;
    try {
      const [missionList, dailySummary] = await Promise.all([
        getMissions(),
        getDailySummary(user.id, date),
      ]);
      setMissions(missionList);
      setSummary(dailySummary);
    } catch {
      setError("데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [user, date]);

  useEffect(() => {
    load();
  }, [load]);

  const handleGenerateFeedback = async () => {
    if (!user) return;
    setFeedbackLoading(true);
    try {
      const feedback = await generateAiFeedback(user.id, date);
      setSummary((s) => (s ? { ...s, aiFeedback: feedback } : s));
    } catch {
      setError("AI 피드백 생성에 실패했습니다.");
    } finally {
      setFeedbackLoading(false);
    }
  };

  if (!user) return null;

  return (
    <div className="container">
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

      <div className="stack" style={{ marginTop: 16 }}>
        {missions.map((mission) => (
          <MissionCard
            key={mission.id}
            mission={mission}
            date={date}
            completed={summary?.completedMissions.includes(mission.name) ?? false}
            onSaved={load}
          />
        ))}
      </div>

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
    </div>
  );
}
