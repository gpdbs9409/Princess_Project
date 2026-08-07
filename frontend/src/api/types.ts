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

// frontend/public/capitals/<name>.png - falls back to a solid color card if missing
export const GOAL_TYPE_IMAGE: Record<GoalTypeCode, string> = {
  PHYSICAL: "/capitals/physical.png",
  ECONOMY: "/capitals/economy.png",
  CULTURE: "/capitals/culture.png",
  KNOWLEDGE: "/capitals/knowledge.png",
  LANGUAGE: "/capitals/language.png",
  PSYCHOLOGY: "/capitals/psychology.png",
  SYMBOL: "/capitals/symbol.png",
};

export type MissionType = "DAILY" | "WEEKLY" | "TOTAL";

export interface UserResponse {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
  role: "USER" | "ADMIN";
}

// ---- admin ----

export interface AdminApplicantResponse {
  userId: number;
  nickname: string;
  appliedAt: string;
}

export interface AdminMemberWeekResponse {
  userId: number;
  nickname: string;
  cohort: string | null;
  weekStart: string;
  weekEnd: string;
  successDays: number;
  eligible: boolean;
  paid: boolean;
  amount: number;
  paidAt: string | null;
  isMvp: boolean;
}

export interface AdminMemberResponse {
  userId: number;
  nickname: string;
  cohort: string | null;
}

export interface AdminMvpResponse {
  userId: number;
  nickname: string;
  cohort: string | null;
  weekStart: string;
  note: string | null;
}

export interface AdminAdjustmentResponse {
  id: number;
  userId: number;
  weekStart: string | null;
  statTypeCode: string | null;
  points: number;
  reason: string | null;
  createdAt: string;
}

export interface ProfileStatsResponse {
  recordCount: number;
  totalUsers: number;
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

export interface TodayRecordEntry {
  inputValue: number;
  memo: string | null;
  photoUrl: string | null;
  aiVerified: boolean | null;
}

export interface DailySummaryResponse {
  date: string;
  totalScore: number;
  progress: number;
  statScores: Partial<Record<string, number>>;
  completedMissions: string[];
  remainingMissions: string[];
  todayRecords: Partial<Record<number, TodayRecordEntry>>;
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
  aiVerified?: boolean;
}

export interface UploadResponse {
  url: string;
}

export interface VisionAnalysisResponse {
  likelyValid: boolean;
  reason: string;
  confidence: string;
}
