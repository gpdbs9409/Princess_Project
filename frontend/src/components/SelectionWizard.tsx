import { useState } from "react";
import {
  GOAL_TYPE_LABELS,
  type CatalogGoal,
  type GoalTypeCode,
  type MissionType,
  type ProjectResponse,
  type ProjectSelectionsRequest,
} from "../api/types";

const MISSION_TYPE_LABELS: Record<MissionType, string> = {
  DAILY: "매일",
  WEEKLY: "매주",
  TOTAL: "전체 기간",
};

interface MissionState {
  missionDefinitionId: number;
  name: string;
  selected: boolean;
  targetValue: number;
  unit: string;
  assignedPoints: number;
  missionType: MissionType;
}

interface CustomMissionState {
  key: string;
  name: string;
  targetValue: number;
  unit: string;
  assignedPoints: number;
  missionType: MissionType;
}

interface StatState {
  statTypeId: number;
  name: string;
  selected: boolean;
  missions: MissionState[];
  customMissions: CustomMissionState[];
}

interface GoalState {
  goalTypeCode: GoalTypeCode;
  name: string;
  selected: boolean;
  weightPercent: number;
  stats: StatState[];
}

let customMissionKeySeq = 0;
function nextCustomMissionKey(): string {
  customMissionKeySeq += 1;
  return `custom-${customMissionKeySeq}`;
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
            const existingMission = existingStat?.missions.find((m) => m.missionDefinitionId === mission.id);
            return {
              missionDefinitionId: mission.id,
              name: mission.name,
              selected: !!existingMission,
              targetValue: existingMission?.targetValue ?? mission.defaultTargetValue,
              unit: existingMission?.unit ?? mission.unit,
              assignedPoints: existingMission?.assignedPoints ?? mission.defaultAssignedPoints,
              missionType: existingMission?.missionType ?? "DAILY",
            };
          }),
          customMissions: (existingStat?.missions ?? [])
            .filter((m) => m.missionDefinitionId === null)
            .map((m) => ({
              key: nextCustomMissionKey(),
              name: m.name,
              targetValue: m.targetValue,
              unit: m.unit,
              assignedPoints: m.assignedPoints,
              missionType: m.missionType,
            })),
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

  const updateStat = (goalTypeCode: GoalTypeCode, statTypeId: number, update: (s: StatState) => StatState) => {
    setGoals((prev) =>
      prev.map((g) =>
        g.goalTypeCode !== goalTypeCode
          ? g
          : { ...g, stats: g.stats.map((s) => (s.statTypeId === statTypeId ? update(s) : s)) }
      )
    );
  };

  const toggleGoal = (goalTypeCode: GoalTypeCode) => {
    setGoals((prev) =>
      prev.map((g) => (g.goalTypeCode === goalTypeCode ? { ...g, selected: !g.selected } : g))
    );
  };

  const updateGoalWeight = (goalTypeCode: GoalTypeCode, weightPercent: number) => {
    setGoals((prev) => prev.map((g) => (g.goalTypeCode === goalTypeCode ? { ...g, weightPercent } : g)));
  };

  const toggleStat = (goalTypeCode: GoalTypeCode, statTypeId: number) => {
    updateStat(goalTypeCode, statTypeId, (s) => ({ ...s, selected: !s.selected }));
  };

  const toggleMission = (goalTypeCode: GoalTypeCode, statTypeId: number, missionDefinitionId: number) => {
    updateStat(goalTypeCode, statTypeId, (s) => ({
      ...s,
      missions: s.missions.map((m) =>
        m.missionDefinitionId === missionDefinitionId ? { ...m, selected: !m.selected } : m
      ),
    }));
  };

  const updateMission = (
    goalTypeCode: GoalTypeCode,
    statTypeId: number,
    missionDefinitionId: number,
    patch: Partial<MissionState>
  ) => {
    updateStat(goalTypeCode, statTypeId, (s) => ({
      ...s,
      missions: s.missions.map((m) =>
        m.missionDefinitionId === missionDefinitionId ? { ...m, ...patch } : m
      ),
    }));
  };

  const addCustomMission = (goalTypeCode: GoalTypeCode, statTypeId: number) => {
    updateStat(goalTypeCode, statTypeId, (s) => ({
      ...s,
      customMissions: [
        ...s.customMissions,
        { key: nextCustomMissionKey(), name: "", targetValue: 1, unit: "회", assignedPoints: 10, missionType: "DAILY" },
      ],
    }));
  };

  const removeCustomMission = (goalTypeCode: GoalTypeCode, statTypeId: number, key: string) => {
    updateStat(goalTypeCode, statTypeId, (s) => ({
      ...s,
      customMissions: s.customMissions.filter((m) => m.key !== key),
    }));
  };

  const updateCustomMission = (
    goalTypeCode: GoalTypeCode,
    statTypeId: number,
    key: string,
    patch: Partial<CustomMissionState>
  ) => {
    updateStat(goalTypeCode, statTypeId, (s) => ({
      ...s,
      customMissions: s.customMissions.map((m) => (m.key === key ? { ...m, ...patch } : m)),
    }));
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
            missions: [
              ...s.missions
                .filter((m) => m.selected)
                .map((m) => ({
                  missionDefinitionId: m.missionDefinitionId,
                  targetValue: m.targetValue,
                  unit: m.unit,
                  assignedPoints: m.assignedPoints,
                  missionType: m.missionType,
                })),
              ...s.customMissions
                .filter((m) => m.name.trim().length > 0)
                .map((m) => ({
                  customName: m.name.trim(),
                  targetValue: m.targetValue,
                  unit: m.unit,
                  assignedPoints: m.assignedPoints,
                  missionType: m.missionType,
                })),
            ],
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
        미션마다 매일/매주 중 인증 주기를 고를 수 있고, 목록에 없는 나만의 미션도 직접 추가할 수 있어요.
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
                        <div key={mission.missionDefinitionId} className="row-between" style={{ fontSize: 13 }}>
                          <label className="row" style={{ gap: 6 }}>
                            <input
                              type="checkbox"
                              checked={mission.selected}
                              onChange={() => toggleMission(goal.goalTypeCode, stat.statTypeId, mission.missionDefinitionId)}
                            />
                            {mission.name}
                          </label>
                          {mission.selected && (
                            <span className="row" style={{ gap: 4 }}>
                              <select
                                value={mission.missionType}
                                onChange={(e) =>
                                  updateMission(goal.goalTypeCode, stat.statTypeId, mission.missionDefinitionId, {
                                    missionType: e.target.value as MissionType,
                                  })
                                }
                                style={{ fontSize: 12, padding: "4px 6px" }}
                              >
                                <option value="DAILY">{MISSION_TYPE_LABELS.DAILY}</option>
                                <option value="WEEKLY">{MISSION_TYPE_LABELS.WEEKLY}</option>
                              </select>
                              <input
                                type="number"
                                value={mission.targetValue}
                                onChange={(e) =>
                                  updateMission(goal.goalTypeCode, stat.statTypeId, mission.missionDefinitionId, {
                                    targetValue: Number(e.target.value) || 0,
                                  })
                                }
                                style={{ maxWidth: 60 }}
                              />
                              <span className="muted">{mission.unit}</span>
                            </span>
                          )}
                        </div>
                      ))}

                      {stat.customMissions.map((mission) => (
                        <div key={mission.key} className="row" style={{ gap: 4, fontSize: 13 }}>
                          <input
                            type="text"
                            placeholder="나만의 미션 이름"
                            value={mission.name}
                            onChange={(e) =>
                              updateCustomMission(goal.goalTypeCode, stat.statTypeId, mission.key, { name: e.target.value })
                            }
                            style={{ flex: 1 }}
                          />
                          <select
                            value={mission.missionType}
                            onChange={(e) =>
                              updateCustomMission(goal.goalTypeCode, stat.statTypeId, mission.key, {
                                missionType: e.target.value as MissionType,
                              })
                            }
                            style={{ fontSize: 12, padding: "4px 6px" }}
                          >
                            <option value="DAILY">{MISSION_TYPE_LABELS.DAILY}</option>
                            <option value="WEEKLY">{MISSION_TYPE_LABELS.WEEKLY}</option>
                          </select>
                          <input
                            type="number"
                            value={mission.targetValue}
                            onChange={(e) =>
                              updateCustomMission(goal.goalTypeCode, stat.statTypeId, mission.key, {
                                targetValue: Number(e.target.value) || 0,
                              })
                            }
                            style={{ maxWidth: 60 }}
                          />
                          <input
                            type="text"
                            value={mission.unit}
                            onChange={(e) =>
                              updateCustomMission(goal.goalTypeCode, stat.statTypeId, mission.key, { unit: e.target.value })
                            }
                            style={{ maxWidth: 50 }}
                          />
                          <button
                            type="button"
                            className="ghost"
                            style={{ padding: "4px 8px" }}
                            onClick={() => removeCustomMission(goal.goalTypeCode, stat.statTypeId, mission.key)}
                          >
                            삭제
                          </button>
                        </div>
                      ))}

                      <button
                        type="button"
                        className="ghost"
                        style={{ alignSelf: "flex-start", padding: "4px 10px", fontSize: 12 }}
                        onClick={() => addCustomMission(goal.goalTypeCode, stat.statTypeId)}
                      >
                        + 나만의 미션 추가
                      </button>
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
