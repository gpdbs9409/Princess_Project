import { useState } from "react";
import { ApiError } from "../api/client";
import { useToast } from "./ToastProvider";
import {
  GOAL_TYPE_LABELS,
  type CatalogGoal,
  type GoalTypeCode,
  type MissionType,
  type ProjectResponse,
  type ProjectSelectionsRequest,
} from "../api/types";

const SAVE_ERROR_MESSAGES: Record<string, string> = {
  WEIGHT_SUM_INVALID: "습관자본 비중의 합계가 100%가 아니어서 저장할 수 없어요. 비중 합계를 100%로 맞춰주세요.",
  DUPLICATE_GOAL_TYPE: "같은 습관자본이 중복 선택되어 있어서 저장할 수 없어요. 중복된 항목을 확인해주세요.",
  UNKNOWN_GOAL_TYPE: "선택한 습관자본 정보를 찾을 수 없어서 저장할 수 없어요. 새로고침 후 다시 시도해주세요.",
  STAT_TYPE_NOT_FOUND: "선택한 행동양식 정보를 찾을 수 없어서 저장할 수 없어요. 새로고침 후 다시 시도해주세요.",
  CUSTOM_STAT_NAME_REQUIRED: "나만의 미션에 이름이 비어 있어서 저장할 수 없어요. 미션 이름을 입력해주세요.",
  MISSION_DEFINITION_NOT_FOUND: "선택한 미션 정보를 찾을 수 없어서 저장할 수 없어요. 새로고침 후 다시 시도해주세요.",
  CONSTRAINT_VIOLATION: "입력한 값이 조건에 맞지 않아서 저장할 수 없어요. 비중(%)과 목표값을 확인해주세요.",
  GOALS_ALREADY_SET: "습관자본과 미션은 이미 설정되어 있어서 다시 저장할 수 없어요. 최초 설정 후에는 수정할 수 없어요.",
};

function saveErrorMessage(err: unknown): string {
  if (err instanceof ApiError && err.code && SAVE_ERROR_MESSAGES[err.code]) {
    return SAVE_ERROR_MESSAGES[err.code];
  }
  return "저장에 실패했어요. 네트워크 상태를 확인하고 다시 시도해주세요.";
}

const MISSION_TYPE_LABELS: Record<MissionType, string> = {
  DAILY: "매일",
  WEEKLY: "매주",
  TOTAL: "전체 기간",
};

const CUSTOM_STAT_NAME = "나만의 미션";

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
  missions: MissionState[];
}

interface CustomStatState {
  missions: CustomMissionState[];
}

interface GoalState {
  goalTypeCode: GoalTypeCode;
  name: string;
  selected: boolean;
  weightPercent: number;
  stats: StatState[];
  customStat: CustomStatState;
}

let customMissionKeySeq = 0;
function nextCustomMissionKey(): string {
  customMissionKeySeq += 1;
  return `custom-${customMissionKeySeq}`;
}

function blankCustomMission(): CustomMissionState {
  return { key: nextCustomMissionKey(), name: "", targetValue: 1, unit: "회", assignedPoints: 10, missionType: "DAILY" };
}

function buildInitialState(catalog: CatalogGoal[], project?: ProjectResponse): GoalState[] {
  return catalog.map((goal) => {
    const existingGoal = project?.goals.find((g) => g.goalTypeCode === goal.code);
    const existingCustomStat = existingGoal?.stats.find((s) => s.statTypeId === null);

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
        };
      }),
      customStat: {
        missions: (existingCustomStat?.missions ?? []).map((m) => ({
          key: nextCustomMissionKey(),
          name: m.name,
          targetValue: m.targetValue,
          unit: m.unit,
          assignedPoints: m.assignedPoints,
          missionType: m.missionType,
        })),
      },
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
  const { showToast } = useToast();
  const [goalHuman, setGoalHuman] = useState(initialProject?.goalHuman ?? "");
  const [goalEnding, setGoalEnding] = useState(initialProject?.goalEnding ?? "");
  const [goals, setGoals] = useState<GoalState[]>(() => buildInitialState(catalog, initialProject));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateGoal = (goalTypeCode: GoalTypeCode, update: (g: GoalState) => GoalState) => {
    setGoals((prev) => prev.map((g) => (g.goalTypeCode === goalTypeCode ? update(g) : g)));
  };

  const updateStat = (goalTypeCode: GoalTypeCode, statTypeId: number, update: (s: StatState) => StatState) => {
    updateGoal(goalTypeCode, (g) => ({
      ...g,
      stats: g.stats.map((s) => (s.statTypeId === statTypeId ? update(s) : s)),
    }));
  };

  const toggleGoal = (goalTypeCode: GoalTypeCode) => {
    updateGoal(goalTypeCode, (g) => {
      const selected = !g.selected;
      return { ...g, selected, weightPercent: selected && g.weightPercent < 1 ? 100 : g.weightPercent };
    });
  };

  const updateGoalWeight = (goalTypeCode: GoalTypeCode, weightPercent: number) => {
    updateGoal(goalTypeCode, (g) => ({ ...g, weightPercent }));
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

  const addCustomMission = (goalTypeCode: GoalTypeCode) => {
    updateGoal(goalTypeCode, (g) => ({
      ...g,
      customStat: { ...g.customStat, missions: [...g.customStat.missions, blankCustomMission()] },
    }));
  };

  const removeCustomMission = (goalTypeCode: GoalTypeCode, key: string) => {
    updateGoal(goalTypeCode, (g) => ({
      ...g,
      customStat: { ...g.customStat, missions: g.customStat.missions.filter((m) => m.key !== key) },
    }));
  };

  const updateCustomMission = (goalTypeCode: GoalTypeCode, key: string, patch: Partial<CustomMissionState>) => {
    updateGoal(goalTypeCode, (g) => ({
      ...g,
      customStat: {
        ...g.customStat,
        missions: g.customStat.missions.map((m) => (m.key === key ? { ...m, ...patch } : m)),
      },
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const selectedGoals = goals
      .filter((g) => g.selected)
      .map((g) => {
        const catalogStats = g.stats
          .map((s) => ({
            statTypeId: s.statTypeId,
            missions: s.missions
              .filter((m) => m.selected)
              .map((m) => ({
                missionDefinitionId: m.missionDefinitionId,
                targetValue: m.targetValue,
                unit: m.unit,
                assignedPoints: m.assignedPoints,
                missionType: m.missionType,
              })),
          }))
          .filter((s) => s.missions.length > 0);

        const customMissions = g.customStat.missions
          .filter((m) => m.name.trim().length > 0)
          .map((m) => ({
            customName: m.name.trim(),
            targetValue: m.targetValue,
            unit: m.unit,
            assignedPoints: m.assignedPoints,
            missionType: m.missionType,
          }));

        const stats = [
          ...catalogStats,
          ...(customMissions.length > 0 ? [{ customStatName: CUSTOM_STAT_NAME, missions: customMissions }] : []),
        ];

        return { goalTypeCode: g.goalTypeCode, weightPercent: g.weightPercent, stats };
      })
      .filter((g) => g.stats.length > 0);

    if (selectedGoals.length === 0) {
      setError("최소 하나의 습관자본에서, 행동양식과 미션을 하나 이상 선택해주세요.");
      return;
    }
    if (selectedGoals.some((g) => g.weightPercent < 1)) {
      setError("선택한 습관자본의 비중(%)은 1 이상이어야 합니다.");
      return;
    }
    const weightSum = selectedGoals.reduce((sum, g) => sum + g.weightPercent, 0);
    if (weightSum !== 100) {
      setError(
        `습관자본 비중의 합계가 100%가 아니어서 저장할 수 없어요. 현재 합계는 ${weightSum}%예요. 100%가 되도록 비중을 조정해주세요.`
      );
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({ goalHuman: goalHuman || undefined, goalEnding: goalEnding || undefined, goals: selectedGoals });
      showToast("저장되었어요");
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="stack" onSubmit={handleSubmit}>
      <a href="/guide.html" target="_blank" rel="noopener noreferrer" className="link">
        가이드라인 보기 →
      </a>
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
        목록에 없는 행동양식은 "나만의 미션" 항목을 체크해서 직접 추가하세요.
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
                  min={1}
                  max={100}
                  value={goal.weightPercent}
                  onChange={(e) => updateGoalWeight(goal.goalTypeCode, Number(e.target.value) || 1)}
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
                  <span className="muted" style={{ fontSize: 13.5, fontWeight: 700 }}>
                    {stat.name}
                  </span>

                  <div className="stack" style={{ marginTop: 6, gap: 6, paddingLeft: 8 }}>
                    {stat.missions.map((mission) => (
                      <div key={mission.missionDefinitionId} className="row-between" style={{ fontSize: 13, flexWrap: "wrap", gap: 6 }}>
                        <label className="row" style={{ gap: 6 }}>
                          <input
                            type="checkbox"
                            checked={mission.selected}
                            onChange={() => toggleMission(goal.goalTypeCode, stat.statTypeId, mission.missionDefinitionId)}
                          />
                          {mission.name}
                        </label>
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
                      </div>
                    ))}
                  </div>
                </div>
              ))}

              {/* "나만의 미션" sits as its own section, a sibling of the catalog stats above
                  (신체 -> 운동/식단/수면/나만의 미션), not nested inside any one of them. */}
              <div>
                <span className="muted" style={{ fontSize: 13.5, fontWeight: 700 }}>
                  {CUSTOM_STAT_NAME}
                </span>

                <div className="stack" style={{ marginTop: 6, gap: 10, paddingLeft: 8 }}>
                  {goal.customStat.missions.map((mission) => (
                      <div key={mission.key} className="stack" style={{ gap: 6, fontSize: 13 }}>
                        <div className="row" style={{ gap: 6 }}>
                          <input
                            type="text"
                            placeholder="미션 이름"
                            value={mission.name}
                            onChange={(e) => updateCustomMission(goal.goalTypeCode, mission.key, { name: e.target.value })}
                            style={{ flex: 1 }}
                          />
                          <button
                            type="button"
                            className="ghost"
                            style={{ padding: "4px 8px" }}
                            onClick={() => removeCustomMission(goal.goalTypeCode, mission.key)}
                          >
                            삭제
                          </button>
                        </div>
                        <div className="row" style={{ gap: 6 }}>
                          <select
                            value={mission.missionType}
                            onChange={(e) =>
                              updateCustomMission(goal.goalTypeCode, mission.key, { missionType: e.target.value as MissionType })
                            }
                            style={{ fontSize: 12, padding: "4px 6px", maxWidth: 90 }}
                          >
                            <option value="DAILY">{MISSION_TYPE_LABELS.DAILY}</option>
                            <option value="WEEKLY">{MISSION_TYPE_LABELS.WEEKLY}</option>
                          </select>
                          <input
                            type="number"
                            value={mission.targetValue}
                            onChange={(e) =>
                              updateCustomMission(goal.goalTypeCode, mission.key, { targetValue: Number(e.target.value) || 0 })
                            }
                            style={{ maxWidth: 60 }}
                          />
                          <input
                            type="text"
                            placeholder="단위"
                            value={mission.unit}
                            onChange={(e) => updateCustomMission(goal.goalTypeCode, mission.key, { unit: e.target.value })}
                            style={{ maxWidth: 60 }}
                          />
                        </div>
                      </div>
                    ))}

                  <button
                    type="button"
                    className="ghost"
                    style={{ alignSelf: "flex-start", padding: "4px 10px", fontSize: 12 }}
                    onClick={() => addCustomMission(goal.goalTypeCode)}
                  >
                    + 미션 추가
                  </button>
                </div>
              </div>
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
