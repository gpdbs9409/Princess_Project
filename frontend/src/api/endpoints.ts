import { api } from "./client";
import type {
  CatalogGoal,
  DailySummaryResponse,
  LoginResponse,
  AiFeedbackResponse,
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

export const getUser = (userId: number) => api.get<UserResponse>(`/api/users/${userId}`);

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
