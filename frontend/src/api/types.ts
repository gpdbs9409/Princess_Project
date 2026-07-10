export type StatType =
  | "PHYSICAL"
  | "ECONOMY"
  | "CULTURE"
  | "KNOWLEDGE"
  | "LANGUAGE"
  | "PSYCHOLOGY"
  | "SYMBOL";

export const STAT_LABELS: Record<StatType, string> = {
  PHYSICAL: "신체",
  ECONOMY: "경제",
  CULTURE: "문화",
  KNOWLEDGE: "지식",
  LANGUAGE: "언어",
  PSYCHOLOGY: "심리",
  SYMBOL: "상징",
};

export const STAT_TYPES: StatType[] = [
  "PHYSICAL",
  "ECONOMY",
  "CULTURE",
  "KNOWLEDGE",
  "LANGUAGE",
  "PSYCHOLOGY",
  "SYMBOL",
];

export type MissionType = "DAILY" | "WEEKLY" | "TOTAL";

export interface UserResponse {
  id: number;
  nickname: string;
  goalHuman: string | null;
  goalEnding: string | null;
  statFocus: Partial<Record<StatType, number>>;
}

export interface LoginResponse {
  token: string;
  user: UserResponse;
}

export interface MissionResponse {
  id: number;
  name: string;
  missionType: MissionType;
  statType: StatType;
  assignedPoints: number;
  targetValue: number;
  unit: string;
  common: boolean;
}

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
  userId: number;
  missionId: number;
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
