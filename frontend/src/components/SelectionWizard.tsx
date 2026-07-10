import { useState } from "react";
import {
  GOAL_TYPE_LABELS,
  type CatalogGoal,
  type GoalTypeCode,
  type ProjectResponse,
  type ProjectSelectionsRequest,
} from "../api/types";

interface MissionState {
  missionDefinitionId: number;
  name: string;
  selected: boolean;
  targetValue: number;
  unit: string;
  assignedPoints: number;
}

interface StatState {
  statTypeId: number;
  name: string;
  selected: boolean;
  missions: MissionState[];
}

interface GoalState {
  goalTypeCode: GoalTypeCode;
  name: string;
  selected: boolean;
  weightPercent: number;
  stats: StatState[];
}

function buildInitialState(catalog: CatalogGoal[], project?: ProjectResponse): GoalState[] {
  return catalog.map((goal) => {
    const existingGoal = project?.goals.find((g) => g.goalTypeCode === goal.code);
    return {
      goalTypeCode: goal.code,
      name: goal.name,
      selected: !!existingGoal,
      weightPercent: existingGoal?.weightPercent ?? 0,
      stats: goal.stats.map((stat) => {
        const existingStat = existingGoal?.stats.find((s) => s.statTypeId === stat.id);
        return {
          statTypeId: stat.id,
          name: stat.name,
          selected: !!existingStat,
          missions: stat.missions.map((mission) => {
            const existingMission = existingStat?.missions.find((m) => m.name === mission.name);
            return {
              missionDefinitionId: mission.id,
              name: mission.name,
              selected: !!existingMission,
              targetValue: existingMission?.targetValue ?? mission.defaultTargetValue,
              unit: existingMission?.unit ?? mission.unit,
              assignedPoints: existingMission?.assignedPoints ?? mission.defaultAssignedPoints,
            };
          }),
        };
      }),
    };
  });
}

interface SelectionWizardProps {
  catalog: CatalogGoal[];
  initialProject?: ProjectResponse;
  submitLabel: string;
  onSubmit: (request: ProjectSelectionsRequest) => Promise<void>;
}

export function SelectionWizard({ catalog, initialProject, submitLabel, onSubmit }: SelectionWizardProps) {
  const [goalHuman, setGoalHuman] = useState(initialProject?.goalHuman ?? "");
  const [goalEnding, setGoalEnding] = useState(initialProject?.goalEnding ?? "");
  const [goals, setGoals] = useState<GoalState[]>(() => buildInitialState(catalog, initialProject));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggleGoal = (goalTypeCode: GoalTypeCode) => {
    setGoals((prev) =>
      prev.map((g) => (g.goalTypeCode === goalTypeCode ? { ...g, selected: !g.selected } : g))
    );
  };

  const updateGoalWeight = (goalTypeCode: GoalTypeCode, weightPercent: number) => {
    setGoals((prev) => prev.map((g) => (g.goalTypeCode === goalTypeCode ? { ...g, weightPercent } : g)));
  };

  const toggleStat = (goalTypeCode: GoalTypeCode, statTypeId: number) => {
    setGoals((prev) =>
      prev.map((g) =>
        g.goalTypeCode !== goalTypeCode
          ? g
          : {
              ...g,
              stats: g.stats.map((s) => (s.statTypeId === statTypeId ? { ...s, selected: !s.selected } : s)),
            }
      )
    );
  };

  const toggleMission = (goalTypeCode: GoalTypeCode, statTypeId: number, missionDefinitionId: number) => {
    setGoals((prev) =>
      prev.map((g) =>
        g.goalTypeCode !== goalTypeCode
          ? g
          : {
              ...g,
              stats: g.stats.map((s) =>
                s.statTypeId !== statTypeId
                  ? s
                  : {
                      ...s,
                      missions: s.missions.map((m) =>
                        m.missionDefinitionId === missionDefinitionId ? { ...m, selected: !m.selected } : m
                      ),
                    }
              ),
            }
      )
    );
  };

  const updateMissionTarget = (
    goalTypeCode: GoalTypeCode,
    statTypeId: number,
    missionDefinitionId: number,
    targetValue: number
  ) => {
    setGoals((prev) =>
      prev.map((g) =>
        g.goalTypeCode !== goalTypeCode
          ? g
          : {
              ...g,
              stats: g.stats.map((s) =>
                s.statTypeId !== statTypeId
                  ? s
                  : {
                      ...s,
                      missions: s.missions.map((m) =>
                        m.missionDefinitionId === missionDefinitionId ? { ...m, targetValue } : m
                      ),
                    }
              ),
            }
      )
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const selectedGoals = goals
      .filter((g) => g.selected)
      .map((g) => ({
        goalTypeCode: g.goalTypeCode,
        weightPercent: g.weightPercent,
        stats: g.stats
          .filter((s) => s.selected)
          .map((s) => ({
            statTypeId: s.statTypeId,
            missions: s.missions
              .filter((m) => m.selected)
              .map((m) => ({
                missionDefinitionId: m.missionDefinitionId,
                targetValue: m.targetValue,
                unit: m.unit,
                assignedPoints: m.assignedPoints,
              })),
          }))
          .filter((s) => s.missions.length > 0),
      }))
      .filter((g) => g.stats.length > 0);

    if (selectedGoals.length === 0) {
      setError("최소 하나의 습관자본에서, 행동양식과 미션을 하나 이상 선택해주세요.");
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({ goalHuman: goalHuman || undefined, goalEnding: goalEnding || undefined, goals: selectedGoals });
    } catch {
      setError("저장에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="stack" onSubmit={handleSubmit}>
      <div className="card stack">
        <div className="stack" style={{ gap: 6 }}>
          <label>이상적인 나의 모습 (선택)</label>
          <input type="text" value={goalHuman} onChange={(e) => setGoalHuman(e.target.value)} placeholder="예: 꾸준히 성장하는 사람" />
        </div>
        <div className="stack" style={{ gap: 6 }}>
          <label>목표로 하는 행동양식 (선택)</label>
          <input type="text" value={goalEnding} onChange={(e) => setGoalEnding(e.target.value)} placeholder="예: 매일 작은 습관을 쌓는 사람" />
        </div>
      </div>

      <p className="muted">
        키우고 싶은 습관자본을 고르고 비중(%)을 입력한 뒤, 그 안에서 행동양식과 구체적인 미션(독서/운동 등)을 선택하세요.
      </p>

      {goals.map((goal) => (
        <div className="card" key={goal.goalTypeCode}>
          <div className="row-between">
            <label className="row" style={{ gap: 8 }}>
              <input type="checkbox" checked={goal.selected} onChange={() => toggleGoal(goal.goalTypeCode)} />
              <strong>{GOAL_TYPE_LABELS[goal.goalTypeCode]}</strong>
            </label>
            {goal.selected && (
              <div className="row" style={{ gap: 6 }}>
                <input
                  type="number"
                  min={0}
                  max={100}
                  value={goal.weightPercent}
                  onChange={(e) => updateGoalWeight(goal.goalTypeCode, Number(e.target.value) || 0)}
                  style={{ maxWidth: 70 }}
                />
                <span className="muted">%</span>
              </div>
            )}
          </div>

          {goal.selected && (
            <div className="stack" style={{ marginTop: 12, gap: 10, paddingLeft: 8, borderLeft: "2px solid var(--border)" }}>
              {goal.stats.map((stat) => (
                <div key={stat.statTypeId}>
                  <label className="row" style={{ gap: 8 }}>
                    <input
                      type="checkbox"
                      checked={stat.selected}
                      onChange={() => toggleStat(goal.goalTypeCode, stat.statTypeId)}
                    />
                    <span style={{ fontSize: 13.5 }}>{stat.name}</span>
                  </label>

                  {stat.selected && (
                    <div className="stack" style={{ marginTop: 6, gap: 6, paddingLeft: 24 }}>
                      {stat.missions.map((mission) => (
                        <label key={mission.missionDefinitionId} className="row-between" style={{ fontSize: 13 }}>
                          <span className="row" style={{ gap: 6 }}>
                            <input
                              type="checkbox"
                              checked={mission.selected}
                              onChange={() => toggleMission(goal.goalTypeCode, stat.statTypeId, mission.missionDefinitionId)}
                            />
                            {mission.name}
                          </span>
                          {mission.selected && (
                            <span className="row" style={{ gap: 4 }}>
                              <input
                                type="number"
                                value={mission.targetValue}
                                onChange={(e) =>
                                  updateMissionTarget(
                                    goal.goalTypeCode,
                                    stat.statTypeId,
                                    mission.missionDefinitionId,
                                    Number(e.target.value) || 0
                                  )
                                }
                                style={{ maxWidth: 70 }}
                              />
                              <span className="muted">{mission.unit}</span>
                            </span>
                          )}
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}

      {error && <div className="error-banner">{error}</div>}
      <button type="submit" className="primary" disabled={submitting}>
        {submitting ? "저장 중..." : submitLabel}
      </button>
    </form>
  );
}
