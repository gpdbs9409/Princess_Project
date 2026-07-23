export type GoalTypeCode =
  | "PHYSICAL"
  | "ECONOMY"
  | "CULTURE"
  | "KNOWLEDGE"
  | "LANGUAGE"
  | "PSYCHOLOGY"
  | "SYMBOL";

export const GOAL_TYPE_LABELS: Record<GoalTypeCode, string> = {
  PHYSICAL: "신체",
  ECONOMY: "경제",
  CULTURE: "문화",
  KNOWLEDGE: "지식",
  LANGUAGE: "언어",
  PSYCHOLOGY: "심리",
  SYMBOL: "상징",
};

export const GOAL_TYPE_CODES: GoalTypeCode[] = [
  "PHYSICAL",
  "ECONOMY",
  "CULTURE",
  "KNOWLEDGE",
  "LANGUAGE",
  "PSYCHOLOGY",
  "SYMBOL",
];

export const GOAL_TYPE_EMOJI: Record<GoalTypeCode, string> = {
  PHYSICAL: "🏃",
  ECONOMY: "💰",
  CULTURE: "🎭",
  KNOWLEDGE: "📚",
  LANGUAGE: "🗣️",
  PSYCHOLOGY: "🧘",
  SYMBOL: "💎",
};

export type MissionType = "DAILY" | "WEEKLY" | "TOTAL";

export interface UserResponse {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
}

export interface LoginResponse {
  token: string;
  user: UserResponse;
}

// ---- catalog (read-only reference tree) ----

export interface CatalogMission {
  id: number;
  name: string;
  description: string | null;
  missionType: MissionType;
  defaultTargetValue: number;
  unit: string;
  defaultAssignedPoints: number;
  requiresPhoto: boolean;
}

export interface CatalogStat {
  id: number;
  code: string;
  name: string;
  description: string | null;
  missions: CatalogMission[];
}

export interface CatalogGoal {
  id: number;
  code: GoalTypeCode;
  name: string;
  description: string | null;
  stats: CatalogStat[];
}

// ---- project (the user's actual selections) ----

export interface ProjectMission {
  id: number;
  missionDefinitionId: number | null;
  name: string;
  targetValue: number;
  unit: string;
  assignedPoints: number;
  missionType: MissionType;
  requiresPhoto: boolean;
}

export interface ProjectStat {
  id: number;
  statTypeId: number | null;
  name: string;
  weightPercent: number | null;
  missions: ProjectMission[];
}

export interface ProjectGoal {
  id: number;
  goalTypeCode: GoalTypeCode;
  name: string;
  weightPercent: number;
  stats: ProjectStat[];
}

export type ProjectStatus = "ACTIVE" | "COMPLETED" | "PAUSED" | "CANCELLED";

export interface ProjectResponse {
  id: number;
  title: string;
  goalHuman: string | null;
  goalEnding: string | null;
  status: ProjectStatus;
  goals: ProjectGoal[];
}

export interface MissionSelectionInput {
  missionDefinitionId?: number;
  customName?: string;
  targetValue: number;
  unit: string;
  assignedPoints: number;
  missionType: MissionType;
}

export interface StatSelectionInput {
  statTypeId?: number;
  weightPercent?: number;
  customStatName?: string;
  missions: MissionSelectionInput[];
}

export interface GoalSelectionInput {
  goalTypeCode: GoalTypeCode;
  weightPercent: number;
  customGoalText?: string;
  stats: StatSelectionInput[];
}

export interface ProjectSelectionsRequest {
  goalHuman?: string;
  goalEnding?: string;
  goals: GoalSelectionInput[];
}

// ---- daily records / reports ----

export interface AiFeedbackResponse {
  summary: string;
  praise: string;
  improvement: string;
  tomorrow: string;
  cheer: string;
}

export interface DailySummaryResponse {
  date: string;
  totalScore: number;
  progress: number;
  statScores: Partial<Record<string, number>>;
  completedMissions: string[];
  remainingMissions: string[];
  aiFeedback: AiFeedbackResponse | null;
}

export interface WeeklyReportResponse {
  weekStart: string;
  weekEnd: string;
  totalScore: number;
  averageProgress: number;
  statScoreTotals: Partial<Record<string, number>>;
  missionCompletionCounts: Partial<Record<string, number>>;
  dailyBreakdown: DailySummaryResponse[];
}

export interface RecordRequest {
  userMissionId: number;
  date: string;
  inputValue: number;
  photoUrl?: string;
  memo?: string;
}

export interface UploadResponse {
  url: string;
}

export interface VisionAnalysisResponse {
  likelyValid: boolean;
  reason: string;
  confidence: string;
}
