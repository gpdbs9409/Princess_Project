import { api } from "./client";
import type {
  AdminAdjustmentResponse,
  AdminApplicantResponse,
  AdminMemberResponse,
  AdminMemberWeekResponse,
  AdminMvpResponse,
  CatalogGoal,
  DailySummaryResponse,
  LoginResponse,
  AiFeedbackResponse,
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

export const signup = (nickname: string, password: string) =>
  api.post<LoginResponse>("/api/auth/signup", { nickname, password });

export const getUser = (userId: number) => api.get<UserResponse>(`/api/users/${userId}`);

export const getProfileStats = (userId: number) =>
  api.get<ProfileStatsResponse>(`/api/users/${userId}/profile-stats`);

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

export const getWeeklyReport = (weekStart: string) =>
  api.get<WeeklyReportResponse>(`/api/projects/active/weekly-report?weekStart=${weekStart}`);

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
