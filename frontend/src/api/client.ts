const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  code?: string;
  constructor(status: number, message: string, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

function getToken(): string | null {
  return localStorage.getItem("princess_token");
}

async function request<T>(
  path: string,
  options: { method?: string; body?: unknown; isMultipart?: boolean } = {}
): Promise<T> {
  const { method = "GET", body, isMultipart = false } = options;
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let requestBody: BodyInit | undefined;
  if (isMultipart) {
    requestBody = body as FormData;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    requestBody = JSON.stringify(body);
  }

  const res = await fetch(`${API_BASE_URL}${path}`, { method, headers, body: requestBody });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    let code: string | undefined;
    let message = text || `요청 실패 (${res.status})`;
    if (text) {
      try {
        const parsed = JSON.parse(text) as { code?: string; message?: string };
        if (parsed.code) code = parsed.code;
        if (parsed.message) message = parsed.message;
      } catch {
        // not a JSON body - fall back to raw text
      }
    }
    throw new ApiError(res.status, message, code);
  }
  if (res.status === 204) return null as T;
  return (await res.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: "PUT", body }),
  postMultipart: <T>(path: string, formData: FormData) =>
    request<T>(path, { method: "POST", body: formData, isMultipart: true }),
  putMultipart: <T>(path: string, formData: FormData) =>
    request<T>(path, { method: "PUT", body: formData, isMultipart: true }),
};
