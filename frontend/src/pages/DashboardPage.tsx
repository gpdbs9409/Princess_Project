import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getActiveProject, getWeeklyReport } from "../api/endpoints";
import {
  GOAL_TYPE_CODES,
  GOAL_TYPE_IMAGE,
  GOAL_TYPE_LABELS,
  type ProjectResponse,
  type WeeklyReportResponse,
} from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { ProfileHeader } from "../components/ProfileHeader";
import { SideWidget } from "../components/SideWidget";
import { StatMeter } from "../components/StatMeter";
import { WeeklyBarChart } from "../components/WeeklyBarChart";

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

const GREETINGS = [
  "오늘도 성장 중이에요",
  "오늘 하루도 빛나볼까요",
  "작은 습관이 큰 변화를 만들어요",
  "오늘의 나를 응원해요",
  "한 걸음씩, 꾸준히 나아가는 중이에요",
  "오늘도 예쁜 하루 보내요",
  "당신의 노력을 지켜보고 있어요",
  "오늘도 자기 자신에게 진심인 당신",
  "습관이 곧 미래가 됩니다",
  "오늘 하루, 나답게 채워봐요",
  "매일이 쌓여 인생이 됩니다",
  "오늘도 리스트업! 체크체크",
  "작은 노력들이 모여 반짝이는 중이에요",
  "오늘의 습관자본을 쌓아볼까요",
  "성장하는 오늘의 당신, 멋져요",
  "오늘도 나만의 속도로 천천히",
  "습관 하나가 하루를 바꿔요",
  "오늘도 최선을 다하는 당신에게 박수를",
  "루틴이 곧 실력이 됩니다",
  "오늘 하루도 알차게 보내봐요",
  "당신의 하루를 응원할게요",
  "오늘도 한 뼘 더 성장 중이에요",
  "꾸준함이 진짜 실력이에요",
  "오늘도 자신과의 약속을 지켜봐요",
  "매일 조금씩, 확실하게 나아가는 중이에요",
  "오늘의 기록이 내일의 자신감이 됩니다",
  "습관이 쌓이면 자존감이 됩니다",
  "오늘도 스스로를 가꾸는 시간이에요",
  "작은 성취가 모여 큰 자신감이 돼요",
  "오늘 하루도 나답게, 반짝이게",
];

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
  const [greeting] = useState(() => GREETINGS[Math.floor(Math.random() * GREETINGS.length)]);

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
      <ProfileHeader />

      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">My Dashboard</span>
        <h1 style={{ fontSize: 28 }}>{user.nickname}님, {greeting}</h1>
        <div className="hub-header-band">
          “{project?.goalHuman || "오늘의 한 걸음이 내일의 나를 만듭니다"}”
        </div>
      </div>

      {error && <div className="error-banner" style={{ marginBottom: 16 }}>{error}</div>}
      {loading && <p className="muted">불러오는 중...</p>}

      <div className="section">
        <div className="section-band">습관자본 바로가기</div>
        <div className="capital-grid">
          {GOAL_TYPE_CODES.filter((code) => project?.goals.some((g) => g.goalTypeCode === code)).map((code) => {
            const goal = project?.goals.find((g) => g.goalTypeCode === code);
            return (
              <button
                key={code}
                type="button"
                className="capital-card is-selected"
                style={{
                  backgroundImage: `linear-gradient(180deg, rgba(0,0,0,0.05) 40%, rgba(0,0,0,0.6)), url(${GOAL_TYPE_IMAGE[code]})`,
                }}
                onClick={() => navigate("/record")}
              >
                <span className="capital-card-label">{GOAL_TYPE_LABELS[code]}</span>
                <span className="capital-card-sub">비중 {goal?.weightPercent}%</span>
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
