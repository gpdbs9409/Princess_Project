import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getActiveProject, getWeeklyReport } from "../api/endpoints";
import {
  GOAL_TYPE_CODES,
  GOAL_TYPE_EMOJI,
  GOAL_TYPE_LABELS,
  type ProjectResponse,
  type WeeklyReportResponse,
} from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { SideWidget } from "../components/SideWidget";
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
  const navigate = useNavigate();
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
      <SideWidget project={project} />

      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">My Dashboard</span>
        <h1 style={{ fontSize: 28 }}>{user.nickname}님, 오늘도 성장 중이에요 🌷</h1>
        <div className="hub-header-band">
          “{project?.goalHuman || "오늘의 한 걸음이 내일의 나를 만듭니다"}”
        </div>
      </div>

      {error && <div className="error-banner" style={{ marginBottom: 16 }}>{error}</div>}
      {loading && <p className="muted">불러오는 중...</p>}

      <div className="section">
        <div className="section-band">습관자본 바로가기</div>
        <div className="capital-grid">
          {GOAL_TYPE_CODES.map((code) => {
            const goal = project?.goals.find((g) => g.goalTypeCode === code);
            const selected = !!goal;
            return (
              <button
                key={code}
                type="button"
                className={`capital-card ${selected ? "is-selected" : "is-unselected"}`}
                onClick={() => navigate(selected ? "/record" : "/stat-focus")}
              >
                <span className="capital-card-emoji">{GOAL_TYPE_EMOJI[code]}</span>
                <span className="capital-card-label">{GOAL_TYPE_LABELS[code]}</span>
                <span className="capital-card-sub">{selected ? `비중 ${goal.weightPercent}%` : "설정하기 →"}</span>
              </button>
            );
          })}
        </div>
      </div>

      {today && (
        <div className="section">
          <div className="section-band">오늘 총점</div>
          <div className="card">
            <div className="row-between">
              <strong className="tabular" style={{ fontSize: 24, fontWeight: 700 }}>
                {Math.round(today.totalScore)}점
              </strong>
              <span className="muted">오늘 달성률 {Math.round(today.progress * 100)}%</span>
            </div>
          </div>
        </div>
      )}

      {today && (
        <div className="section">
          <div className="section-band">오늘의 스탯</div>
          <div className="card">
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
        <div className="section">
          <div className="section-band">최근 7일 총점 추이</div>
          <div className="card">
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
        </div>
      )}

      {report && (
        <div className="section">
          <div className="section-band">이번 주 스탯 누적</div>
          <div className="card">
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
    </div>
  );
}
