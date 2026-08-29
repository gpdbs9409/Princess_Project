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

// Mirrors the backend's UserProjectService bounds (MIN/MAX_MISSION_TARGET) - kept here too so
// out-of-range numbers (e.g. "-50" or "999999" typed into a "30분" mission) get clamped as the
// user types instead of only failing once they hit save.
const MIN_GOAL_WEIGHT = 1;
const MAX_GOAL_WEIGHT = 100;
const MIN_MISSION_TARGET = 0.1;
const MAX_MISSION_TARGET = 100000;

function clamp(value: number, min: number, max: number): number {
  if (Number.isNaN(value)) return min;
  return Math.min(max, Math.max(min, value));
}

const SAVE_ERROR_MESSAGES: Record<string, string> = {
  WEIGHT_SUM_INVALID: "아비투스 비중의 합계가 100%가 아니어서 저장할 수 없어요. 비중 합계를 100%로 맞춰주세요.",
  GOAL_WEIGHT_OUT_OF_RANGE: "아비투스 비중(%)은 1~100 사이여야 해요. 값을 확인해주세요.",
  DUPLICATE_GOAL_TYPE: "같은 아비투스이 중복 선택되어 있어서 저장할 수 없어요. 중복된 항목을 확인해주세요.",
  UNKNOWN_GOAL_TYPE: "선택한 아비투스 정보를 찾을 수 없어서 저장할 수 없어요. 새로고침 후 다시 시도해주세요.",
  STAT_TYPE_NOT_FOUND: "선택한 행동양식 정보를 찾을 수 없어서 저장할 수 없어요. 새로고침 후 다시 시도해주세요.",
  CUSTOM_STAT_NAME_REQUIRED: "나만의 미션에 이름이 비어 있어서 저장할 수 없어요. 미션 이름을 입력해주세요.",
  MISSION_DEFINITION_NOT_FOUND: "선택한 미션 정보를 찾을 수 없어서 저장할 수 없어요. 새로고침 후 다시 시도해주세요.",
  MISSION_TARGET_OUT_OF_RANGE: `미션 목표값이 올바르지 않아요. 0보다 크고 ${MAX_MISSION_TARGET.toLocaleString()} 이하로 입력해주세요.`,
  MISSION_POINTS_OUT_OF_RANGE: "미션 배점이 올바르지 않아요. 값을 확인해주세요.",
  CONSTRAINT_VIOLATION: "입력한 값이 조건에 맞지 않아서 저장할 수 없어요. 비중(%)과 목표값을 확인해주세요.",
  GOALS_ALREADY_SET: "아비투스와 미션은 이미 설정되어 있어서 다시 저장할 수 없어요. 최초 설정 후에는 수정할 수 없어요.",
  GOAL_COUNT_INVALID: "아비투스는 3개까지만 선택할 수 있어요.",
};

function saveErrorMessage(err: unknown): string {
  if (err instanceof ApiError && err.code && SAVE_ERROR_MESSAGES[err.code]) {
    return SAVE_ERROR_MESSAGES[err.code];
  }
  return "저장에 실패했어요. 네트워크 상태를 확인하고 다시 시도해주세요.";
}

const CUSTOM_STAT_NAME = "나만의 미션";
const CUSTOM_STAT_EXAMPLES: Record<GoalTypeCode, string> = {
  PHYSICAL: "회복 관리",
  ECONOMY: "투자 관리",
  CULTURE: "예술 감상",
  KNOWLEDGE: "복습 관리",
  LANGUAGE: "회화 연습",
  PSYCHOLOGY: "감정 관리",
  SYMBOL: "퍼스널 브랜딩",
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
  selected: boolean;
  saved: boolean;
  // null이면 customSectionName으로 묶인 "나만의 세부 항목"에 들어간다. 값이 있으면 그
  // statTypeId를 가진 기존 세부 항목(운동/식단/수면 등) 밑에 바로 얹혀서, 그 항목의 카탈로그
  // 미션들과 나란히 표시된다 (2026-08-26 QA: 세부 항목마다 미션을 추가할 수 있으면 좋겠다는
  // 요청 반영 - 백엔드는 이미 statTypeId + customName 조합을 지원해서 이 필드 하나로 충분하다).
  targetStatTypeId: number | null;
  // targetStatTypeId가 null일 때만 쓰인다. 같은 이름을 적은 미션끼리 하나의 커스텀 세부
  // 항목으로 묶여 저장된다 - 기본값 "나만의 미션"을 그대로 두면 예전과 동일하게 동작하고,
  // 이름을 직접 바꾸면 그게 곧 새로운 세부 항목 이름이 된다 (세부 항목 직접 추가 요청 반영).
  customSectionName: string;
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
  return {
    key: nextCustomMissionKey(),
    name: "",
    targetValue: 1,
    unit: "회",
    // 배점은 더 이상 미션에 붙지 않는다. 서버가 자본 비중(%)에서 계산하므로 여기서는
    // 0을 보내고, 컬럼은 하위호환을 위해서만 남아 있다.
    assignedPoints: 0,
    missionType: "DAILY",
    selected: true,
    saved: false,
    targetStatTypeId: null,
    customSectionName: CUSTOM_STAT_NAME,
  };
}

// "독서" is already one of the 3 mandatory common tasks (공통 과제 - see the notice
// rendered above the goal list), so it's excluded here to avoid asking people to pick it
// again as if it were an optional 지식 mission.
const COMMON_TASK_STAT_NAMES = ["독서"];

// React's controlled <input type="number"> only re-writes the DOM's displayed text when
// the *parsed* numeric value actually changes - typing a leading zero in front of an
// existing value (e.g. "30" -> "030") still parses to the same number, so the stray zero
// silently stays on screen until blur. Stripping it from the raw string and writing it
// back onto the input immediately (before React ever gets involved) fixes that for good.
function sanitizeNumberInput(e: React.ChangeEvent<HTMLInputElement>): string {
  const cleaned = e.target.value.replace(/^0+(?=\d)/, "");
  e.target.value = cleaned;
  return cleaned;
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
      stats: goal.stats.filter((stat) => !COMMON_TASK_STAT_NAMES.includes(stat.name)).map((stat) => {
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
          selected: true,
          saved: true,
          targetStatTypeId: null,
          customSectionName: CUSTOM_STAT_NAME,
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
  const [goalAppearance, setGoalAppearance] = useState(initialProject?.goalAppearance ?? "");
  const [goalEnding, setGoalEnding] = useState(initialProject?.goalEnding ?? "");
  const [readingBookTitle, setReadingBookTitle] = useState(initialProject?.commonReadingBookTitle ?? "");
  const [readingTotalPages, setReadingTotalPages] = useState(
    initialProject?.commonReadingTotalPages?.toString() ?? ""
  );
  const [studyYoutubeTopic, setStudyYoutubeTopic] = useState(initialProject?.commonStudyYoutubeTopic ?? "");
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
    const selectedCount = goals.filter((goal) => goal.selected).length;
    const target = goals.find((goal) => goal.goalTypeCode === goalTypeCode);
    if (target && !target.selected && selectedCount >= 3) {
      setError("아비투스는 정확히 3개만 선택할 수 있어요.");
      return;
    }
    setError(null);
    updateGoal(goalTypeCode, (g) => {
      const selected = !g.selected;
      return { ...g, selected, weightPercent: selected && g.weightPercent < 1 ? 100 : g.weightPercent };
    });
  };

  const updateGoalWeight = (goalTypeCode: GoalTypeCode, weightPercent: number) => {
    updateGoal(goalTypeCode, (g) => ({ ...g, weightPercent: clamp(weightPercent, MIN_GOAL_WEIGHT, MAX_GOAL_WEIGHT) }));
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
      customStat: {
        ...g.customStat,
        missions: [
          ...g.customStat.missions,
          { ...blankCustomMission(), customSectionName: CUSTOM_STAT_EXAMPLES[goalTypeCode] },
        ],
      },
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

  const saveCustomMission = (goalTypeCode: GoalTypeCode, mission: CustomMissionState) => {
    if (!mission.name.trim()) {
      setError("추가할 미션 이름을 입력해주세요.");
      return;
    }
    if (mission.targetStatTypeId == null && !mission.customSectionName.trim()) {
      setError("새 세부 항목 이름을 입력해주세요.");
      return;
    }
    updateCustomMission(goalTypeCode, mission.key, {
      name: mission.name.trim(),
      customSectionName: mission.customSectionName.trim(),
      saved: true,
      selected: true,
    });
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const selectedGoals = goals
      .filter((g) => g.selected)
      .map((g) => {
        // 나만의 미션 중 "기존 세부 항목 밑에 추가"를 고른 것들은 statTypeId별로 묶어서,
        // 아래에서 그 항목의 카탈로그 미션들과 합쳐 하나의 StatSelection으로 보낸다 (백엔드는
        // 이미 같은 stat 안에 missionDefinitionId 미션과 customName 미션이 섞이는 걸 지원한다).
        const customByStatTypeId = new Map<number, { customName: string; targetValue: number; unit: string; assignedPoints: number; missionType: MissionType }[]>();
        // "새 세부 항목 만들기"를 고른 것들은 적어둔 이름으로 묶는다 - 같은 이름을 적으면 같은
        // 세부 항목 하나로 합쳐지고, 이름을 바꾸면 그게 곧 새 세부 항목이 된다.
        const customByNewSectionName = new Map<string, { customName: string; targetValue: number; unit: string; assignedPoints: number; missionType: MissionType }[]>();

        g.customStat.missions
          .filter((m) => m.saved && m.selected && m.name.trim().length > 0)
          .forEach((m) => {
            const entry = {
              customName: m.name.trim(),
              targetValue: m.targetValue,
              unit: m.unit,
              assignedPoints: m.assignedPoints,
              missionType: "DAILY" as const,
            };
            if (m.targetStatTypeId != null) {
              const list = customByStatTypeId.get(m.targetStatTypeId) ?? [];
              list.push(entry);
              customByStatTypeId.set(m.targetStatTypeId, list);
            } else {
              const sectionName = m.customSectionName.trim() || CUSTOM_STAT_NAME;
              const list = customByNewSectionName.get(sectionName) ?? [];
              list.push(entry);
              customByNewSectionName.set(sectionName, list);
            }
          });

        const catalogStats = g.stats
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
                  missionType: "DAILY" as const,
                })),
              ...(customByStatTypeId.get(s.statTypeId) ?? []),
            ],
          }))
          .filter((s) => s.missions.length > 0);

        const newSectionStats = Array.from(customByNewSectionName.entries()).map(([customStatName, missions]) => ({
          customStatName,
          missions,
        }));

        const stats = [...catalogStats, ...newSectionStats];

        return { goalTypeCode: g.goalTypeCode, weightPercent: g.weightPercent, stats };
      })
      .filter((g) => g.stats.length > 0);

    if (selectedGoals.length === 0) {
      setError("최소 하나의 아비투스에서, 행동양식과 미션을 하나 이상 선택해주세요.");
      return;
    }
    if (selectedGoals.length !== 3) {
      setError(`아비투스는 정확히 3개를 선택해야 해요. 현재 ${selectedGoals.length}개를 선택했어요.`);
      return;
    }
    if (selectedGoals.some((g) => g.weightPercent < 1)) {
      setError("선택한 아비투스의 비중(%)은 1 이상이어야 합니다.");
      return;
    }
    const weightSum = selectedGoals.reduce((sum, g) => sum + g.weightPercent, 0);
    if (weightSum !== 100) {
      setError(
        `아비투스 비중의 합계가 100%가 아니어서 저장할 수 없어요. 현재 합계는 ${weightSum}%예요. 100%가 되도록 비중을 조정해주세요.`
      );
      return;
    }
    const totalPages = Number(readingTotalPages);
    if (!readingBookTitle.trim() || !readingTotalPages || !Number.isInteger(totalPages) || totalPages < 1 || totalPages > 100000) {
      setError("공통자본의 책 제목과 전체 분량(1~100,000쪽)을 입력해주세요.");
      return;
    }
    if (!studyYoutubeTopic.trim()) {
      setError("공통자본의 공부 목표 YouTube 주제를 입력해주세요.");
      return;
    }

    if (!window.confirm("아비투스와 미션은 한 번 설정하면 수정할 수 없어요. 이대로 확정할까요?")) return;
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({
        goalHuman: goalHuman || undefined,
        goalAppearance: goalAppearance || undefined,
        goalEnding: goalEnding || undefined,
        commonReadingBookTitle: readingBookTitle.trim(),
        commonReadingTotalPages: totalPages,
        commonStudyYoutubeTopic: studyYoutubeTopic.trim(),
        goals: selectedGoals,
      });
      showToast("저장되었어요");
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const selectedGoalsForSum = goals.filter((g) => g.selected);
  const weightSumLive = selectedGoalsForSum.reduce((sum, g) => sum + g.weightPercent, 0);
  const weightSumOk = weightSumLive === 100;

  return (
    // noValidate: this form uses number inputs with min/max (weight 1-100, mission target
    // range) purely as numeric-keyboard hints. Without it, some mobile browsers/in-app
    // webviews (e.g. KakaoTalk's) silently block the submit event on an out-of-range value
    // instead of reliably showing their native validation bubble - from the user's side that
    // looks exactly like "save does nothing, no error, my input is gone". All of the actual
    // validation below already happens in JS with a visible error banner, so native
    // constraint validation would only ever get in the way here.
    <form className="stack" onSubmit={handleSubmit} noValidate>
      <a href="/guide.html" target="_blank" rel="noopener noreferrer" className="link">
        가이드라인 보기 →
      </a>
      <div className="card stack">
        <div className="stack" style={{ gap: 6 }}>
          <label>내가 되고 싶은 인간상</label>
          <input
            type="text"
            value={goalHuman}
            onChange={(e) => setGoalHuman(e.target.value)}
            placeholder="예: 자기 기준이 분명한 여자"
          />
        </div>
        <div className="stack" style={{ gap: 6 }}>
          <label>외적 페르소나</label>
          <input
            type="text"
            value={goalAppearance}
            onChange={(e) => setGoalAppearance(e.target.value)}
            placeholder="예: 승무원 이미지"
          />
        </div>
        <div className="stack" style={{ gap: 6 }}>
          <label>내가 살고 싶은 삶</label>
          <input
            type="text"
            value={goalEnding}
            onChange={(e) => setGoalEnding(e.target.value)}
            placeholder="예: 내 취향의 공간·음식·라이프스타일을 소개하고 영향력을 끼칠 수 있는 삶"
          />
        </div>
      </div>

      <div className="card stack" style={{ gap: 14 }}>
        <div>
          <strong>공통자본</strong>
          <div className="muted">아비투스 선택과 별도로 모든 참가자가 매일 수행해요.</div>
        </div>
        <div className="stack" style={{ gap: 8 }}>
          <strong style={{ fontSize: 14 }}>독서</strong>
          <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
            <input type="text" value={readingBookTitle} onChange={(e) => setReadingBookTitle(e.target.value)}
              placeholder="책 제목" maxLength={200} style={{ flex: "1 1 220px" }} />
            <input type="number" value={readingTotalPages} onChange={(e) => setReadingTotalPages(sanitizeNumberInput(e))}
              placeholder="책 전체 분량" min={1} max={100000} style={{ flex: "0 1 160px" }} />
            <span className="muted">쪽</span>
          </div>
        </div>
        <div className="stack" style={{ gap: 8 }}>
          <strong style={{ fontSize: 14 }}>공부</strong>
          <input type="text" value={studyYoutubeTopic} onChange={(e) => setStudyYoutubeTopic(e.target.value)}
            placeholder="나에게 영감을 주는 YouTube 주제 (예: 경제 뉴스 쉽게 이해하기)" maxLength={300} />
        </div>
      </div>

      <p className="muted">
        키우고 싶은 아비투스를 고르고 비중(%)을 입력한 뒤, 그 안에서 행동양식과 구체적인 미션(운동 등)을 선택하세요.
        목록에 없는 미션은 "미션 직접 추가"에서 만들 수 있는데, 이때 기존 세부 항목(운동/식단/수면 등) 밑에
        바로 넣을지, 이름을 새로 지어 새 세부 항목을 만들지 고를 수 있어요.
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
                  min={MIN_GOAL_WEIGHT}
                  max={MAX_GOAL_WEIGHT}
                  value={goal.weightPercent}
                  onChange={(e) => updateGoalWeight(goal.goalTypeCode, Number(sanitizeNumberInput(e)) || 0)}
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
                          <input
                            type="number"
                            min={MIN_MISSION_TARGET}
                            max={MAX_MISSION_TARGET}
                            value={mission.targetValue}
                            onChange={(e) =>
                              updateMission(goal.goalTypeCode, stat.statTypeId, mission.missionDefinitionId, {
                                targetValue: clamp(Number(sanitizeNumberInput(e)) || 0, MIN_MISSION_TARGET, MAX_MISSION_TARGET),
                              })
                            }
                            style={{ maxWidth: 60 }}
                          />
                          <span className="muted">{mission.unit}</span>
                        </span>
                      </div>
                    ))}
                    {goal.customStat.missions
                      .filter((mission) => mission.saved && mission.targetStatTypeId === stat.statTypeId)
                      .map((mission) => (
                        <label key={mission.key} className="row" style={{ gap: 6, fontSize: 13 }}>
                          <input
                            type="checkbox"
                            checked={mission.selected}
                            onChange={() =>
                              updateCustomMission(goal.goalTypeCode, mission.key, { selected: !mission.selected })
                            }
                          />
                          {mission.name}
                        </label>
                      ))}
                  </div>
                </div>
              ))}

              {Array.from(new Set(
                goal.customStat.missions
                  .filter((mission) => mission.saved && mission.targetStatTypeId == null)
                  .map((mission) => mission.customSectionName)
              )).map((sectionName) => (
                <div key={sectionName}>
                  <span className="muted" style={{ fontSize: 13.5, fontWeight: 700 }}>
                    {sectionName}
                  </span>
                  <div className="stack" style={{ marginTop: 6, gap: 6, paddingLeft: 8 }}>
                    {goal.customStat.missions
                      .filter((mission) =>
                        mission.saved && mission.targetStatTypeId == null && mission.customSectionName === sectionName
                      )
                      .map((mission) => (
                        <label key={mission.key} className="row" style={{ gap: 6, fontSize: 13 }}>
                          <input
                            type="checkbox"
                            checked={mission.selected}
                            onChange={() =>
                              updateCustomMission(goal.goalTypeCode, mission.key, { selected: !mission.selected })
                            }
                          />
                          {mission.name}
                        </label>
                      ))}
                  </div>
                </div>
              ))}

              {/* 사용자가 기존 세부 항목에 미션을 붙이거나 새 세부 항목을 만들 수 있는 영역. */}
              <div>
                <span className="muted" style={{ fontSize: 13.5, fontWeight: 700 }}>
                  미션 직접 추가
                </span>

                <div className="stack" style={{ marginTop: 6, gap: 10, paddingLeft: 8 }}>
                  {goal.customStat.missions.filter((mission) => !mission.saved).map((mission) => (
                      <div key={mission.key} className="stack" style={{ gap: 6, fontSize: 13 }}>
                        {/* 어디에 반영할지: 기존 세부 항목(운동/식단/수면 등) 중 하나를 고르면 그
                            항목 밑에 바로 붙고, "새 세부 항목"을 고르면 아래 이름 입력칸에 적은
                            이름으로 새 세부 항목이 만들어진다 (2026-08-26 QA 반영). */}
                        <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                          <select
                            value={mission.targetStatTypeId ?? "custom"}
                            onChange={(e) =>
                              updateCustomMission(goal.goalTypeCode, mission.key, {
                                targetStatTypeId: e.target.value === "custom" ? null : Number(e.target.value),
                                customSectionName:
                                  e.target.value === "custom" && !mission.customSectionName.trim()
                                    ? CUSTOM_STAT_EXAMPLES[goal.goalTypeCode]
                                    : mission.customSectionName,
                              })
                            }
                            aria-label="미션을 추가할 세부 항목"
                            style={{ fontSize: 13, maxWidth: 180, height: 40 }}
                          >
                            {goal.stats.map((s) => (
                              <option key={s.statTypeId} value={s.statTypeId}>
                                {s.name}
                              </option>
                            ))}
                            <option value="custom">새 세부 항목 만들기</option>
                          </select>
                          {mission.targetStatTypeId == null && (
                            <input
                              type="text"
                              aria-label="새 세부 항목 이름"
                              placeholder={`세부 항목 (예: ${CUSTOM_STAT_EXAMPLES[goal.goalTypeCode]})`}
                              value={mission.customSectionName}
                              onChange={(e) =>
                                updateCustomMission(goal.goalTypeCode, mission.key, { customSectionName: e.target.value })
                              }
                              style={{ maxWidth: 180, height: 40 }}
                            />
                          )}
                        </div>
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
                            className="primary"
                            style={{ padding: "4px 12px" }}
                            onClick={() => saveCustomMission(goal.goalTypeCode, mission)}
                          >
                            저장
                          </button>
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

      {/* Live running total, shown as soon as anything is selected - checking a 2nd/3rd
          capital defaults each one to 100% (see toggleGoal), so most people hit a sum != 100%
          through completely normal use, not typos. Surfacing it here means they see and fix
          it before ever hitting "저장하기", instead of only finding out after a failed submit. */}
      {selectedGoalsForSum.length > 0 && (
        <div
          className="row-between"
          style={{
            padding: "10px 14px",
            borderRadius: 10,
            fontSize: 13.5,
            fontWeight: 600,
            background: weightSumOk ? "var(--good-soft)" : "var(--warn-soft)",
            color: weightSumOk ? "var(--good)" : "var(--warn)",
          }}
        >
          <span>선택한 아비투스 비중 합계</span>
          <span>
            {weightSumLive}% {weightSumOk ? "✓" : "· 100%가 되도록 맞춰주세요"}
          </span>
        </div>
      )}

      {error && <div className="error-banner">{error}</div>}
      <button type="submit" className="primary" disabled={submitting}>
        {submitting ? "저장 중..." : submitLabel}
      </button>
    </form>
  );
}
