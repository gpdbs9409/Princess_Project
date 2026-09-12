import { useEffect, useState } from "react";
import { getActiveProject, getDailySummary } from "../api/endpoints";
import { GOAL_TYPE_CODES, GOAL_TYPE_LABELS, type DailySummaryResponse, type ProjectResponse } from "../api/types";
import { CAPITAL_COLORS, dailyMaxByGoal } from "../lib/capitalScore";

function todayIso(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

interface CapitalSegment {
  code: string;
  label: string;
  value: number;
  max: number;
  color: string;
}

interface CapitalSegmentWithPercent extends CapitalSegment {
  percent: number;
}

// 레오집사 채팅 상단에 "지금까지의 자본 점수"를 한눈에 보여주는 요약 카드 (2026-09, 필라이즈
// 앱 캡처를 참고해달라는 요청). 대시보드가 자본별로 각각 개별 막대를 그리는 것과 달리, 여기서는
// 막대 하나를 오늘 각 자본이 벌어들인 점수 비중대로 나눠 칠하는 스택형 바 + 태그 칩 +
// (오늘 레오집사 피드백이 있다면) 그 한마디를 함께 보여준다.
export function CapitalScoreSummaryCard() {
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [summary, setSummary] = useState<DailySummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    Promise.all([getActiveProject(), getDailySummary(todayIso())])
      .then(([projectData, summaryData]) => {
        setProject(projectData);
        setSummary(summaryData);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, []);

  // 로딩 중/실패 시에는 조용히 아무것도 안 보여준다 - 이 카드는 채팅을 보조하는 요약일 뿐,
  // 못 불러온다고 레오집사 채팅 전체를 막을 이유는 없다.
  if (loading || error || !summary) {
    return null;
  }

  const dailyMax = dailyMaxByGoal(project);
  dailyMax.common = 20;

  const selectedCodes = GOAL_TYPE_CODES.filter((code) =>
    project?.goals.some((goal) => goal.goalTypeCode === code)
  );

  const rawSegments: CapitalSegment[] = [
    ...selectedCodes.map((code) => {
      const key = code.toLowerCase();
      return {
        code: key,
        label: GOAL_TYPE_LABELS[code],
        value: summary.statScores[key] ?? 0,
        max: dailyMax[key] ?? 0,
        color: CAPITAL_COLORS[key] ?? CAPITAL_COLORS.common,
      };
    }),
    {
      code: "common",
      label: "공통(독서·공부)",
      value: summary.statScores.common ?? 0,
      max: dailyMax.common ?? 0,
      color: CAPITAL_COLORS.common,
    },
  ].filter((segment) => segment.max > 0);

  const totalScore = summary.totalScore;
  // 각 조각이 전체 막대에서 차지하는 폭은 그 자본의 "만점 비중"이 아니라 오늘 실제로 얻은 점수의
  // 비중이다 - 그래야 막대 전체 길이가 곧 오늘 총점을 시각적으로 나타내는 필라이즈 스타일이 된다.
  const segments: CapitalSegmentWithPercent[] = rawSegments.map((segment) => ({
    ...segment,
    percent: totalScore > 0 ? (Math.max(0, segment.value) / totalScore) * 100 : 0,
  }));

  const progressPercent = Math.round(summary.progress * 100);
  const latestComment = summary.aiFeedback?.summary ?? null;

  return (
    <div className="card capital-score-card">
      <div className="row-between">
        <div className="capital-score-total">
          <span className="capital-score-total-label">오늘의 자본 점수</span>
          <span className="capital-score-total-value tabular">
            {Math.round(totalScore)}
            <span className="capital-score-total-unit">점</span>
          </span>
        </div>
        <span className="muted">달성률 {progressPercent}%</span>
      </div>

      <div className="capital-score-bar-track" role="img" aria-label="자본별 오늘 점수 비중">
        {segments.map((segment) =>
          segment.percent > 0 ? (
            <div
              key={segment.code}
              className="capital-score-bar-segment"
              style={{ width: `${segment.percent}%`, backgroundColor: segment.color }}
            />
          ) : null
        )}
      </div>

      <div className="capital-score-tags">
        {segments.map((segment) => (
          <span className="capital-score-tag" key={segment.code}>
            <span className="capital-score-tag-dot" style={{ backgroundColor: segment.color }} />
            {segment.label} {Math.round(segment.percent)}%
          </span>
        ))}
      </div>

      {latestComment && <p className="capital-score-comment">🤍 레오집사: {latestComment}</p>}
    </div>
  );
}
