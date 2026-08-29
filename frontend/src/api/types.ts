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

export type Role = "USER" | "ADMIN";

export interface UserResponse {
  id: number;
  nickname: string;
  email: string | null;
  profileImageUrl: string | null;
  role: Role;
  instagram: string | null;
}

// ---- admin ----

export interface AdminApplicantResponse {
  userId: number;
  nickname: string;
  appliedAt: string;
  role: Role;
}

export interface AdminMemberWeekResponse {
  userId: number;
  nickname: string;
  cohort: string | null;
  weekStart: string;
  weekEnd: string;
  successDays: number;
  dailyCredits: number[];
  eligible: boolean;
  paid: boolean;
  amount: number;
  paidAt: string | null;
  isMvp: boolean;
  role: Role;
}

export interface AdminMemberResponse {
  userId: number;
  nickname: string;
  cohort: string | null;
}

export interface AdminActivityResponse {
  id: number;
  userId: number;
  nickname: string;
  activityType: "PERSONAL" | CommonTaskType;
  name: string;
  recordDate: string;
  actualValue: number | null;
  targetValue: number | null;
  unit: string | null;
  earnedScore: number | null;
  achievementRate: number | null;
  detail: string | null;
  memo: string | null;
  photoUrl: string | null;
  aiVerified: boolean | null;
  adminInvalidated: boolean;
  recordedAt: string | null;
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

export type RecruitmentStatus = "PENDING" | "ACCEPTED" | "REJECTED";

export interface RecruitmentApplicantResponse {
  id: number;
  name: string;
  contact: string | null;
  note: string | null;
  status: RecruitmentStatus;
  appliedAt: string | null;
  createdAt: string;
}

export interface RecruitmentApplicantRequest {
  name: string;
  contact?: string;
  note?: string;
  status?: RecruitmentStatus;
  appliedAt?: string;
}

export interface ProfileStatsResponse {
  recordCount: number;
  totalUsers: number;
}

// 팔로워/팔로잉 클릭 시 보여주는 같은 기수 참가자 목록 (실제 팔로우 관계는 없음).
// goalHuman(이상향)/goalAppearance(추구미)는 아직 온보딩을 안 마친 참가자면 null일 수 있다.
export interface ParticipantResponse {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
  goalHuman: string | null;
  goalAppearance: string | null;
  goalEnding: string | null;
  instagram: string | null;
}

export interface LoginResponse {
  token: string;
  user: UserResponse;
}

// ---- 회원가입 이메일 인증 (2026-08-26 요청: 이메일 필수화 + 인증 후 가입) ----

export interface EmailVerificationConfirmResponse {
  verifiedToken: string;
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
  goalAppearance: string | null;
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
  goalAppearance?: string;
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

// 레오집사 채팅(누적 히스토리) 화면용 - AiFeedbackResponse와 필드는 같지만 날짜(feedbackDate)가
// 추가로 붙어서, 여러 날짜를 한번에 리스트로 받아 쭉 이어지는 채팅처럼 렌더링할 수 있다.
export interface AiFeedbackHistoryEntry {
  feedbackDate: string;
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
  /** 그날의 만점. 참가자가 미션 배점을 직접 정하므로 사람마다 다르다. */
  maxPossible: number;
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

// ---- daily common tasks (독서/공부): score/refund inputs ----

export type CommonTaskType = "READING" | "STUDY";

export interface CommonTaskRequest {
  taskType: CommonTaskType;
  date: string;
  // READING 전용, 선택 입력 (2026-08-26 QA: 책 제목도 같이 기록하면 좋겠다는 요청 반영).
  bookTitle?: string;
  startPage?: number;
  endPage?: number;
  studyPlannedAmount?: number;
  studyCompletedAmount?: number;
  // 독서/공부 모두 사진 인증 필수.
  photoUrl?: string;
  // Vision 결과는 저장 차단용이 아니라 운영진 검토용 플래그다.
  aiVerified?: boolean;
  memo?: string;
}

export interface CommonTaskResponse {
  id: number;
  taskType: CommonTaskType;
  recordDate: string;
  bookTitle: string | null;
  startPage: number | null;
  endPage: number | null;
  studyPlannedAmount: number | null;
  studyCompletedAmount: number | null;
  photoUrl: string | null;
  aiVerified: boolean | null;
  memo: string | null;
  createdAt: string;
}

export interface WeeklyRetrospectiveRequest {
  date: string;
  retroDailyLife?: string;
  retroWeekReview?: string;
  retroNextWeekPlan?: string;
}

export interface WeeklyRetrospectiveResponse {
  id: number;
  recordDate: string;
  retroDailyLife: string | null;
  retroWeekReview: string | null;
  retroNextWeekPlan: string | null;
  createdAt: string;
}
