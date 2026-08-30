import { useEffect, useState } from "react";
import { getActiveProject, getProfileStats, updateInstagram, updateProfileImage } from "../api/endpoints";
import type { ProfileStatsResponse, ProjectResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import { ProjectReadOnlyView } from "../components/ProjectReadOnlyView";

export function MyPage() {
  const { user, updateUser } = useAuth();
  const { showToast } = useToast();
  const [stats, setStats] = useState<ProfileStatsResponse | null>(null);
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [editingInstagram, setEditingInstagram] = useState(false);
  const [instagramInput, setInstagramInput] = useState("");
  const [instagramSaving, setInstagramSaving] = useState(false);
  const [instagramError, setInstagramError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    Promise.all([getProfileStats(user.id), getActiveProject()])
      .then(([profileStats, activeProject]) => {
        setStats(profileStats);
        setProject(activeProject);
      })
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
        <strong>이메일</strong>
        <p className="muted" style={{ margin: 0 }}>
          가입 시 인증된 이메일이며 변경할 수 없어요. 비밀번호 재설정 메일도 이 주소로 발송됩니다.
        </p>
        <p style={{ margin: 0 }}>{user.email ?? "인증된 이메일 정보가 없어요."}</p>
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

      {project && project.goals.length > 0 && (
        <section className="section" style={{ marginTop: 28 }}>
          <div className="section-band">나의 아비투스</div>
          <p className="muted" style={{ marginBottom: 14 }}>
            최초 설정한 아비투스와 미션이에요. 설정 후에는 수정할 수 없어요.
          </p>
          <ProjectReadOnlyView project={project} />
        </section>
      )}
    </div>
  );
}
