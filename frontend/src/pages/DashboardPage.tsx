import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getActiveProject, getWeeklyReport } from "../api/endpoints";
import { GOAL_TYPE_CODES, GOAL_TYPE_LABELS, type ProjectResponse, type WeeklyReportResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { StatMeter } from "../components/StatMeter";
import { WeeklyBarChart } from "../components/WeeklyBarChart";

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

function isoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function DashboardPage() {
  const { user } = useAuth();
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [report, setReport] = useState<WeeklyReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const today = new Date();
    const weekStart = new Date(today);
    weekStart.setDate(today.getDate() - 6);

    Promise.all([getActiveProject(), getWeeklyReport(isoDate(weekStart))])
      .then(([projectData, reportData]) => {
        setProject(projectData);
        setReport(reportData);
      })
      .catch(() => setError("데이터를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, []);

  if (!user) return null;

  const today = report?.dailyBreakdown[report.dailyBreakdown.length - 1] ?? null;
  const todayIso = isoDate(new Date());

  const statMax = report
    ? Math.max(1, ...GOAL_TYPE_CODES.map((s) => report.statScoreTotals[s.toLowerCase()] ?? 0))
    : 1;
  const todayStatMax = today
    ? Math.max(1, ...GOAL_TYPE_CODES.map((s) => today.statScores[s.toLowerCase()] ?? 0))
    : 1;

  return (
    <div className="container">
      <div className="row-between" style={{ marginBottom: 20 }}>
        <div>
          <span className="eyebrow">대시보드</span>
          <h1 style={{ fontSize: 26 }}>{user.nickname}님의 성장 기록</h1>
        </div>
        <Link to="/record" className="link">
          오늘 기록하러 가기 →
        </Link>
      </div>

      {error && <div className="error-banner" style={{ marginBottom: 16 }}>{error}</div>}
      {loading && <p className="muted">불러오는 중...</p>}

      {today && (
        <div className="card">
          <div className="row-between">
            <strong>오늘 총점</strong>
            <span className="tabular" style={{ fontSize: 22, fontWeight: 700 }}>
              {Math.round(today.totalScore)}점
            </span>
          </div>
          <p className="muted" style={{ marginTop: 4 }}>
            오늘 달성률 {Math.round(today.progress * 100)}%
          </p>
        </div>
      )}

      {today && (
        <div className="card">
          <h2 style={{ fontSize: 16, marginBottom: 14 }}>오늘의 스탯</h2>
          <div>
            {GOAL_TYPE_CODES.map((s) => (
              <StatMeter
                key={s}
                label={GOAL_TYPE_LABELS[s]}
                value={today.statScores[s.toLowerCase()] ?? 0}
                max={todayStatMax}
              />
            ))}
          </div>
        </div>
      )}

      {report && (
        <div className="card">
          <h2 style={{ fontSize: 16, marginBottom: 4 }}>최근 7일 총점 추이</h2>
          <p className="muted" style={{ marginBottom: 8 }}>
            주간 합계 {Math.round(report.totalScore)}점 · 평균 달성률{" "}
            {Math.round(report.averageProgress * 100)}%
          </p>
          <WeeklyBarChart
            days={report.dailyBreakdown.map((d) => {
              const dow = new Date(d.date + "T00:00:00").getDay();
              return {
                date: d.date,
                label: WEEKDAY_LABELS[dow],
                value: d.totalScore,
                isToday: d.date === todayIso,
              };
            })}
          />
        </div>
      )}

      {report && (
        <div className="card">
          <h2 style={{ fontSize: 16, marginBottom: 14 }}>이번 주 스탯 누적</h2>
          <div>
            {GOAL_TYPE_CODES.map((s) => (
              <StatMeter
                key={s}
                label={GOAL_TYPE_LABELS[s]}
                value={report.statScoreTotals[s.toLowerCase()] ?? 0}
                max={statMax}
              />
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <h2 style={{ fontSize: 16, marginBottom: 10 }}>목표</h2>
        <div className="stack" style={{ gap: 6 }}>
          <p>
            <strong>이상적인 나</strong>: {project?.goalHuman || "설정되지 않음"}
          </p>
          <p>
            <strong>행동양식</strong>: {project?.goalEnding || "설정되지 않음"}
          </p>
        </div>
        <Link to="/stat-focus" className="link" style={{ marginTop: 12, display: "inline-block" }}>
          습관자본·미션 수정
        </Link>
      </div>
    </div>
  );
}
