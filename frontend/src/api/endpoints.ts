import { api } from "./client";
import type {
  AdminAdjustmentResponse,
  AdminActivityResponse,
  AdminApplicantResponse,
  AdminMemberResponse,
  AdminMemberWeekResponse,
  AdminMvpResponse,
  RecruitmentApplicantRequest,
  RecruitmentApplicantResponse,
  CatalogGoal,
  CommonTaskRequest,
  CommonTaskResponse,
  WeeklyRetrospectiveRequest,
  WeeklyRetrospectiveResponse,
  DailySummaryResponse,
  LoginResponse,
  AiFeedbackResponse,
  AiFeedbackHistoryEntry,
  EmailVerificationConfirmResponse,
  ParticipantResponse,
  ProfileStatsResponse,
  ProjectResponse,
  ProjectSelectionsRequest,
  RecordRequest,
  UploadResponse,
  UserResponse,
  VisionAnalysisResponse,
  WeeklyReportResponse,
} from "./types";

export const login = (nickname: string, password: string) =>
  api.post<LoginResponse>("/api/auth/login", { nickname, password });

// 2026-08-26: 이메일이 선택에서 필수로 바뀌었고, 가입 전에 이메일 인증을 통과해야 한다 -
// requestEmailVerification/confirmEmailVerification으로 먼저 인증하고 받은 verifiedToken을
// 여기 실어 보내야 가입이 완료된다 (서버가 다시 한번 검증함).
export const signup = (
  nickname: string,
  password: string,
  email: string,
  emailVerificationToken: string,
  instagram?: string
) =>
  api.post<LoginResponse>("/api/auth/signup", {
    nickname,
    password,
    email,
    emailVerificationToken,
    instagram: instagram?.trim() || undefined,
  });

export const requestEmailVerification = (email: string) =>
  api.post<void>("/api/auth/email-verification/request", { email });

export const confirmEmailVerification = (email: string, code: string) =>
  api.post<EmailVerificationConfirmResponse>("/api/auth/email-verification/confirm", { email, code });

export const forgotPassword = (nickname: string) =>
  api.post<void>("/api/auth/forgot-password", { nickname });

export const resetPassword = (token: string, newPassword: string) =>
  api.post<void>("/api/auth/reset-password", { token, newPassword });

export const getUser = (userId: number) => api.get<UserResponse>(`/api/users/${userId}`);

export const updateEmail = (userId: number, email: string) =>
  api.put<UserResponse>(`/api/users/${userId}/email`, { email });

export const getProfileStats = (userId: number) =>
  api.get<ProfileStatsResponse>(`/api/users/${userId}/profile-stats`);

export const getParticipants = (userId: number) =>
  api.get<ParticipantResponse[]>(`/api/users/${userId}/participants`);

export const updateProfileImage = (userId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.putMultipart<UserResponse>(`/api/users/${userId}/profile-image`, formData);
};

export const getCatalog = () => api.get<CatalogGoal[]>("/api/catalog");

export const getActiveProject = () => api.get<ProjectResponse>("/api/projects/active");

export const replaceSelections = (request: ProjectSelectionsRequest) =>
  api.put<ProjectResponse>("/api/projects/active/selections", request);

export const saveRecord = (record: RecordRequest) => api.post<DailySummaryResponse>("/api/records", record);

export const getDailySummary = (date: string) =>
  api.get<DailySummaryResponse>(`/api/projects/active/daily?date=${date}`);

export const generateAiFeedback = (date: string) =>
  api.post<AiFeedbackResponse>(`/api/projects/active/ai-feedback?date=${date}`);

// 레오집사 채팅(누적 히스토리) - 지금까지 쌓인 모든 날짜의 코멘트를 오래된 순으로 받는다.
export const getAiFeedbackHistory = () =>
  api.get<AiFeedbackHistoryEntry[]>("/api/projects/active/ai-feedback/history");

export const getWeeklyReport = (weekStart: string) =>
  api.get<WeeklyReportResponse>(`/api/projects/active/weekly-report?weekStart=${weekStart}`);

// ---- common tasks (독서/공부/주간회고) ----

export const saveCommonTask = (request: CommonTaskRequest) =>
  api.post<CommonTaskResponse>("/api/common-tasks", request);

export const getDailyCommonTasks = (date: string) =>
  api.get<CommonTaskResponse[]>(`/api/common-tasks/daily?date=${date}`);

export const getWeeklyCommonTask = (weekStart: string) =>
  api.get<WeeklyRetrospectiveResponse | null>(`/api/weekly-retrospectives?weekStart=${weekStart}`);

// 지난 주간회고 히스토리 (2026-08-27 요청) - 이번 주는 getWeeklyCommonTask로 따로 받고, 그 이전
// 주들만 최신순으로 받아서 입력란 아래에 스택으로 쌓는다.
export const getWeeklyRetrospectiveHistory = (weekStart: string) =>
  api.get<WeeklyRetrospectiveResponse[]>(`/api/weekly-retrospectives/history?weekStart=${weekStart}`);

export const saveWeeklyRetrospective = (request: WeeklyRetrospectiveRequest) =>
  api.post<WeeklyRetrospectiveResponse>("/api/weekly-retrospectives", request);

export const updateWeeklyRetrospective = (recordId: number, request: WeeklyRetrospectiveRequest) =>
  api.put<WeeklyRetrospectiveResponse>(`/api/weekly-retrospectives/${recordId}`, request);

export const uploadFile = (file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.postMultipart<UploadResponse>("/api/uploads", formData);
};

export const analyzeVisionPhoto = (file: File, expectedTopic: string) => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("expectedTopic", expectedTopic);
  return api.postMultipart<VisionAnalysisResponse>("/api/vision/analyze", formData);
};

// ---- admin ----

export const getAdminCohorts = () => api.get<string[]>("/api/admin/cohorts");

export const getAdminApplicants = () => api.get<AdminApplicantResponse[]>("/api/admin/applicants");

export const getAdminParticipants = (cohort: string | null, weekStart: string) => {
  const params = new URLSearchParams({ weekStart });
  if (cohort) params.set("cohort", cohort);
  return api.get<AdminMemberWeekResponse[]>(`/api/admin/participants?${params.toString()}`);
};

export const getAdminMemberActivities = (userId: number) =>
  api.get<AdminActivityResponse[]>(`/api/admin/members/${userId}/activities`);

export const getAdminActivitiesForReview = (cohort: string | null) => {
  const query = cohort ? `?cohort=${encodeURIComponent(cohort)}` : "";
  return api.get<AdminActivityResponse[]>(`/api/admin/activities/review${query}`);
};

export const setAdminActivityInvalidated = (activityType: string, recordId: number, invalidated: boolean) =>
  api.put<void>(`/api/admin/activities/${activityType}/${recordId}/invalidation`, { invalidated });

export const assignAdminCohort = (userId: number, cohort: string | null) =>
  api.put<AdminMemberResponse>(`/api/admin/members/${userId}/cohort`, { cohort });

export const setAdminRefundPaid = (userId: number, weekStart: string, paid: boolean) =>
  api.put<AdminMemberWeekResponse>(`/api/admin/members/${userId}/refund?weekStart=${weekStart}`, { paid });

export const setAdminMvp = (userId: number, weekStart: string, note?: string) =>
  api.put<AdminMvpResponse>("/api/admin/mvp", { userId, weekStart, note });

export const clearAdminMvp = (cohort: string, weekStart: string) =>
  api.del<void>(`/api/admin/mvp?cohort=${encodeURIComponent(cohort)}&weekStart=${weekStart}`);

export const addAdminAdjustment = (
  userId: number,
  payload: { weekStart?: string; statTypeCode?: string; points: number; reason?: string }
) => api.post<AdminAdjustmentResponse>(`/api/admin/members/${userId}/adjustments`, payload);

export const getAdminAdjustments = (userId: number) =>
  api.get<AdminAdjustmentResponse[]>(`/api/admin/members/${userId}/adjustments`);

export const deleteAdminAdjustment = (adjustmentId: number) =>
  api.del<void>(`/api/admin/adjustments/${adjustmentId}`);

// ---- recruitment applicants (internal-only, separate from users) ----

export const getRecruitmentApplicants = () =>
  api.get<RecruitmentApplicantResponse[]>("/api/admin/recruitment-applicants");

export const addRecruitmentApplicant = (payload: RecruitmentApplicantRequest) =>
  api.post<RecruitmentApplicantResponse>("/api/admin/recruitment-applicants", payload);

export const addRecruitmentApplicantsBulk = (payload: RecruitmentApplicantRequest[]) =>
  api.post<RecruitmentApplicantResponse[]>("/api/admin/recruitment-applicants/bulk", payload);

export const updateRecruitmentApplicant = (id: number, payload: RecruitmentApplicantRequest) =>
  api.put<RecruitmentApplicantResponse>(`/api/admin/recruitment-applicants/${id}`, payload);

export const deleteRecruitmentApplicant = (id: number) =>
  api.del<void>(`/api/admin/recruitment-applicants/${id}`);
