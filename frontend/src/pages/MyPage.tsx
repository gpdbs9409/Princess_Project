import { useEffect, useState } from "react";
import {
  getActiveProject,
  getActiveReadingBook,
  getProfileStats,
  registerReadingBook,
  updateInstagram,
  updateProfileImage,
} from "../api/endpoints";
import { ApiError } from "../api/client";
import type { ProfileStatsResponse, ProjectResponse, ReadingBookResponse } from "../api/types";
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

  // 독서 - 지금 읽고 있는 책 (2026-09: 완독 후 여기서 새 책을 등록하면 그 책으로 바로 활성화되고,
  // 이전 책은 자동으로 완독 처리된다. 병렬독서는 지원하지 않는다).
  const [readingBook, setReadingBook] = useState<ReadingBookResponse | null>(null);
  const [readingBookLoading, setReadingBookLoading] = useState(true);
  const [registeringBook, setRegisteringBook] = useState(false);
  const [newBookTitle, setNewBookTitle] = useState("");
  const [newBookSaving, setNewBookSaving] = useState(false);
  const [newBookError, setNewBookError] = useState<string | null>(null);

  const loadReadingBook = () => {
    setReadingBookLoading(true);
    getActiveReadingBook()
      .then(setReadingBook)
      .catch(() => {
        // 독서 기록이 아직 하나도 없는 아주 초기 상태 등에서는 조용히 비워둔다 - 아래 카드가
        // "아직 등록된 책이 없어요" 상태로 자연스럽게 보여준다.
      })
      .finally(() => setReadingBookLoading(false));
  };

  useEffect(() => {
    if (!user) return;
    Promise.all([getProfileStats(user.id), getActiveProject()])
      .then(([profileStats, activeProject]) => {
        setStats(profileStats);
        setProject(activeProject);
      })
      .catch(() => setError("정보를 불러오지 못했어요."));
    loadReadingBook();
  }, [user]);

  const startRegisteringBook = () => {
    setNewBookTitle("");
    setNewBookError(null);
    setRegisteringBook(true);
  };

  const handleRegisterBook = async () => {
    if (!newBookTitle.trim()) {
      setNewBookError("책 제목을 입력해주세요.");
      return;
    }
    setNewBookSaving(true);
    setNewBookError(null);
    try {
      const created = await registerReadingBook(newBookTitle.trim());
      setReadingBook(created);
      setRegisteringBook(false);
      showToast("새 책이 등록되었어요. 오늘부터 이 책으로 기록해주세요!");
    } catch (err) {
      setNewBookError(
        err instanceof ApiError && err.code === "BOOK_TITLE_REQUIRED"
          ? "책 제목을 입력해주세요."
          : "책 등록에 실패했어요. 잠시 후 다시 시도해주세요."
      );
    } finally {
      setNewBookSaving(false);
    }
  };

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

      <div className="card stack" style={{ gap: 10, marginTop: 16 }}>
        <div className="row-between">
          <strong>독서 - 지금 읽는 책</strong>
          {!registeringBook && (
            <button type="button" className="ghost" onClick={startRegisteringBook}>
              완독하고 새 책 등록
            </button>
          )}
        </div>
        <p className="muted" style={{ margin: 0 }}>
          한 번에 한 권만 활성화돼요 (병렬독서는 지원하지 않아요). 새 책을 등록하면 지금 책은
          자동으로 완독 처리되고, 오늘의 독서 기록부터 새 책 기준으로 이어서 쓸 수 있어요.
        </p>

        {!readingBookLoading && readingBook && !registeringBook && (
          <div className="recorded-field">
            <span className="muted">{readingBook.title ? "읽고 있는 책" : "아직 책 제목이 없어요"}</span>
            <strong>{readingBook.title || "제목을 등록해주세요"}</strong>
            {readingBook.lastEndPage != null && (
              <span className="muted">마지막으로 기록한 페이지: {readingBook.lastEndPage}p</span>
            )}
          </div>
        )}

        {registeringBook && (
          <div className="stack" style={{ gap: 8 }}>
            <input
              type="text"
              value={newBookTitle}
              onChange={(e) => setNewBookTitle(e.target.value)}
              placeholder="새로 읽을 책 제목"
              maxLength={200}
            />
            {newBookError && <div className="error-banner">{newBookError}</div>}
            <div className="row" style={{ gap: 8 }}>
              <button type="button" className="primary" onClick={handleRegisterBook} disabled={newBookSaving}>
                {newBookSaving ? "등록 중..." : "새 책으로 시작하기"}
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => setRegisteringBook(false)}
                disabled={newBookSaving}
              >
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
