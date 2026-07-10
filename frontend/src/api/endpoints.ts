import { api } from "./client";
import type {
  DailySummaryResponse,
  LoginResponse,
  MissionResponse,
  AiFeedbackResponse,
  RecordRequest,
  StatType,
  UploadResponse,
  UserResponse,
  VisionAnalysisResponse,
  WeeklyReportResponse,
} from "./types";

export const login = (nickname: string) => api.post<LoginResponse>("/api/auth/login", { nickname });

export const getUser = (userId: number) => api.get<UserResponse>(`/api/users/${userId}`);

export const updateStatFocus = (userId: number, stats: { statType: StatType; weightPercent: number }[]) =>
  api.put<UserResponse>(`/api/users/${userId}/stat-focus`, { stats });

export const getMissions = () => api.get<MissionResponse[]>("/api/missions");

export const saveRecord = (record: RecordRequest) => api.post<DailySummaryResponse>("/api/records", record);

export const getDailySummary = (userId: number, date: string) =>
  api.get<DailySummaryResponse>(`/api/users/${userId}/daily?date=${date}`);

export const generateAiFeedback = (userId: number, date: string) =>
  api.post<AiFeedbackResponse>(`/api/users/${userId}/ai-feedback?date=${date}`);

export const getWeeklyReport = (userId: number, weekStart: string) =>
  api.get<WeeklyReportResponse>(`/api/users/${userId}/weekly-report?weekStart=${weekStart}`);

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
