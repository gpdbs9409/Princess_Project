import { useEffect, useState } from "react";
import { ApiError } from "../api/client";
import { getProfileStats, updateEmail, updateInstagram, updateProfileImage } from "../api/endpoints";
import type { ProfileStatsResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";

export function MyPage() {
  const { user, updateUser } = useAuth();
  const { showToast } = useToast();
  const [stats, setStats] = useState<ProfileStatsResponse | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [editingEmail, setEditingEmail] = useState(false);
  const [emailInput, setEmailInput] = useState("");
  const [emailSaving, setEmailSaving] = useState(false);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [editingInstagram, setEditingInstagram] = useState(false);
  const [instagramInput, setInstagramInput] = useState("");
  const [instagramSaving, setInstagramSaving] = useState(false);
  const [instagramError, setInstagramError] = useState<string | null>(null);

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

  const startEditingEmail = () => {
    setEmailInput(user.email ?? "");
    setEmailError(null);
    setEditingEmail(true);
  };

  const handleEmailSave = async () => {
    if (!emailInput.trim()) {
      setEmailError("이메일을 입력해주세요.");
      return;
    }
    setEmailSaving(true);
    setEmailError(null);
    try {
      const updated = await updateEmail(user.id, emailInput.trim());
      updateUser(updated);
      setEditingEmail(false);
    } catch (err) {
      if (err instanceof ApiError && err.code === "EMAIL_TAKEN") {
        setEmailError("이미 사용 중인 이메일이에요.");
      } else if (err instanceof ApiError && err.code === "INVALID_EMAIL") {
        setEmailError("이메일 형식이 올바르지 않아요. 올바른 이메일 주소를 입력해주세요.");
      } else {
        setEmailError("이메일 저장에 실패했어요. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setEmailSaving(false);
    }
  };

  const startEditingInstagram = () => {
    setInstagramInput(user.instagram ?? "");
    setInstagramError(null);
    setEditingInstagram(true);
  };

  const handleInstagramSave = async () => {
    setInstagramSaving(true);
    setInstagramError(null);
    try {
      const updated = await updateInstagram(user.id, instagramInput);
      updateUser(updated);
      setEditingInstagram(false);
      showToast(updated.instagram ? "인스타그램이 저장되었어요" : "인스타그램 등록을 해제했어요");
    } catch {
      setInstagramError("인스타그램 저장에 실패했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setInstagramSaving(false);
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
            <div className="row" style={{ gap: 8, alignItems: "center" }}>
              <strong style={{ fontSize: 18 }}>{user.nickname}</strong>
              {stats?.mvp && <span className="badge good">★ MVP</span>}
            </div>
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

      <div className="card stack" style={{ gap: 10, marginTop: 16 }}>
        <div className="row-between">
          <strong>이메일</strong>
          {!editingEmail && (
            <button type="button" className="ghost" onClick={startEditingEmail}>
              {user.email ? "수정" : "등록"}
            </button>
          )}
        </div>
        <p className="muted" style={{ margin: 0 }}>
          비밀번호를 잊었을 때 재설정 메일을 받는 용도예요.
        </p>

        {!editingEmail && <p style={{ margin: 0 }}>{user.email ?? "등록된 이메일이 없어요."}</p>}

        {editingEmail && (
          <div className="stack" style={{ gap: 8 }}>
            <input
              type="email"
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)}
              placeholder="example@email.com"
            />
            {emailError && <div className="error-banner">{emailError}</div>}
            <div className="row" style={{ gap: 8 }}>
              <button type="button" className="primary" onClick={handleEmailSave} disabled={emailSaving}>
                {emailSaving ? "저장 중..." : "저장"}
              </button>
              <button type="button" className="ghost" onClick={() => setEditingEmail(false)} disabled={emailSaving}>
                취소
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="card stack" style={{ gap: 10, marginTop: 16 }}>
        <div className="row-between">
          <strong>인스타그램</strong>
          {!editingInstagram && (
            <button type="button" className="ghost" onClick={startEditingInstagram}>
              {user.instagram ? "수정" : "등록"}
            </button>
          )}
        </div>
        <p className="muted" style={{ margin: 0 }}>
          함께하는 참가자가 프로필에서 바로 방문할 수 있어요.
        </p>

        {!editingInstagram && (
          user.instagram ? (
            <a href={`https://instagram.com/${user.instagram}`} target="_blank" rel="noreferrer">
              @{user.instagram}
            </a>
          ) : <p style={{ margin: 0 }}>등록된 인스타그램이 없어요.</p>
        )}

        {editingInstagram && (
          <div className="stack" style={{ gap: 8 }}>
            <input
              type="text"
              value={instagramInput}
              onChange={(e) => setInstagramInput(e.target.value)}
              placeholder="@username 또는 인스타그램 주소"
              maxLength={100}
            />
            <p className="muted" style={{ margin: 0 }}>비워서 저장하면 등록이 해제돼요.</p>
            {instagramError && <div className="error-banner">{instagramError}</div>}
            <div className="row" style={{ gap: 8 }}>
              <button type="button" className="primary" onClick={handleInstagramSave} disabled={instagramSaving}>
                {instagramSaving ? "저장 중..." : "저장"}
              </button>
              <button type="button" className="ghost" onClick={() => setEditingInstagram(false)} disabled={instagramSaving}>
                취소
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
