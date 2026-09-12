import type { ProjectResponse } from "../api/types";

/**
 * 자본(아비투스)별 하루 만점. 개인 미션 몫(80점)을 참가자가 고른 자본 비중(%)대로 나눈다 -
 * DailyRecordService의 서버 계산과 동일한 규칙이라야 화면의 바 길이가 실제 달성률과 일치한다.
 * 미션을 하나도 안 고른 자본은 몫이 없다.
 *
 * 대시보드와 레오집사 채팅(CapitalScoreSummaryCard)이 같은 계산을 공유한다 (2026-09).
 */
export function dailyMaxByGoal(project: ProjectResponse | null): Record<string, number> {
  const result: Record<string, number> = {};
  const goalsWithMissions = (project?.goals ?? []).filter((goal) =>
    goal.stats.some((stat) => stat.missions.length > 0)
  );
  const weightSum = goalsWithMissions.reduce((sum, goal) => sum + goal.weightPercent, 0);
  for (const goal of goalsWithMissions) {
    result[goal.goalTypeCode.toLowerCase()] = weightSum > 0
      ? (80 * goal.weightPercent) / weightSum
      : 80 / goalsWithMissions.length;
  }
  return result;
}

// 자본별 고정 색상 (2026-09, 필라이즈 참고 요청) - 매번 같은 자본이 같은 색으로 보이도록 고정한다.
export const CAPITAL_COLORS: Record<string, string> = {
  physical: "#465775",
  economy: "#60748B",
  culture: "#A4899E",
  knowledge: "#91809E",
  language: "#7995A3",
  psychology: "#9AAAC0",
  symbol: "#6C6784",
  common: "var(--accent)",
};
