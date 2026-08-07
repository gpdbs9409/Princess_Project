import { useEffect, useState } from "react";
import { getProfileStats, updateProfileImage } from "../api/endpoints";
import type { ProfileStatsResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";

export function MyPage() {
  const { user, updateUser } = useAuth();
  const [stats, setStats] = useState<ProfileStatsResponse | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    getProfileStats(user.id)
      .then(setStats)
      .catch(() => setError("정보를 불러오지 못했어요."));
  }, [user]);

  if (!user) return null;

  const handlePhotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const updated = await updateProfileImage(user.id, file);
      updateUser(updated);
    } catch {
      setError("사진 변경에 실패했어요.");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="container">
      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">My Page</span>
        <h1 style={{ fontSize: 26 }}>마이페이지</h1>
      </div>

      {error && <div className="error-banner" style={{ marginBottom: 16 }}>{error}</div>}

      <div className="card stack" style={{ gap: 14 }}>
        <div className="row" style={{ gap: 14, alignItems: "center" }}>
          {user.profileImageUrl ? (
            <img
              src={user.profileImageUrl}
              alt="프로필 사진"
              style={{ width: 64, height: 64, borderRadius: "50%", objectFit: "cover" }}
            />
          ) : (
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: "50%",
                background: "var(--surface-alt, #eee)",
              }}
            />
          )}
          <div>
            <strong style={{ fontSize: 18 }}>{user.nickname}</strong>
            <div className="muted">{user.role === "ADMIN" ? "운영진" : "프린세스 프로젝트 멤버"}</div>
          </div>
        </div>

        <div className="row" style={{ gap: 10 }}>
          <label htmlFor="my-page-photo" className="file-picker-button">
            {uploading ? "업로드 중..." : "프로필 사진 변경"}
          </label>
          <input
            id="my-page-photo"
            type="file"
            accept="image/*"
            className="visually-hidden-input"
            onChange={handlePhotoChange}
            disabled={uploading}
          />
        </div>

        {stats && (
          <p className="muted">
            지금까지 기록 {stats.recordCount}개를 남겼어요 · 함께하는 멤버 {stats.totalUsers}명
          </p>
        )}
      </div>
    </div>
  );
}
