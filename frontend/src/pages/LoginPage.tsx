import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError } from "../api/client";
import { getActiveProject, login, signup, updateProfileImage } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";

type Mode = "login" | "signup";

export function LoginPage() {
  const { signIn, updateUser } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>("login");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    return () => {
      if (photoPreviewUrl) URL.revokeObjectURL(photoPreviewUrl);
    };
  }, [photoPreviewUrl]);

  const switchMode = (next: Mode) => {
    setMode(next);
    setError(null);
  };

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setPhoto(file);
    setPhotoPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return file ? URL.createObjectURL(file) : null;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname.trim() || !password) return;
    setLoading(true);
    setError(null);
    try {
      const res = mode === "signup" ? await signup(nickname.trim(), password) : await login(nickname.trim(), password);
      signIn(res.token, res.user);

      if (mode === "signup" && photo) {
        const updated = await updateProfileImage(res.user.id, photo);
        updateUser(updated);
      }

      const project = await getActiveProject();
      if (project.goals.length === 0) {
        navigate("/stat-focus", { state: { welcome: true } });
      } else {
        navigate("/dashboard");
      }
    } catch (err) {
      if (err instanceof ApiError && mode === "login" && err.code === "NICKNAME_NOT_FOUND") {
        setError("존재하지 않는 닉네임이에요. 닉네임을 다시 확인하거나 회원가입해주세요.");
      } else if (err instanceof ApiError && mode === "login" && err.status === 401) {
        setError("비밀번호가 올바르지 않아요.");
      } else if (err instanceof ApiError && mode === "signup" && err.code === "NICKNAME_TAKEN") {
        setError("이미 사용 중인 닉네임이에요. 다른 닉네임을 입력하거나 로그인해주세요.");
      } else {
        setError(mode === "signup" ? "회원가입에 실패했습니다. 잠시 후 다시 시도해주세요." : "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: 420, paddingTop: 96 }}>
      <div className="stack" style={{ marginBottom: 28, textAlign: "center" }}>
        <span className="eyebrow">Princess Diary</span>
        <h1 style={{ fontSize: 30 }}>프린세스 다이어리</h1>
      </div>

      <div className="row" style={{ gap: 8, marginBottom: 16, justifyContent: "center" }}>
        <button
          type="button"
          className={mode === "login" ? "primary" : "ghost"}
          onClick={() => switchMode("login")}
        >
          로그인
        </button>
        <button
          type="button"
          className={mode === "signup" ? "primary" : "ghost"}
          onClick={() => switchMode("signup")}
        >
          회원가입
        </button>
      </div>

      <form className="card stack" onSubmit={handleSubmit}>
        <div className="stack" style={{ gap: 6 }}>
          <label htmlFor="nickname">닉네임</label>
          <input
            id="nickname"
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="예: 공주님"
            autoFocus
          />
        </div>
        <div className="stack" style={{ gap: 6 }}>
          <label htmlFor="password">비밀번호</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호"
          />
        </div>
        {mode === "signup" && (
          <div className="stack" style={{ gap: 8 }}>
            <label htmlFor="photo">본인 사진 (선택)</label>
            {photoPreviewUrl && <img src={photoPreviewUrl} alt="선택한 사진 미리보기" className="photo-preview" />}
            <div className="row" style={{ gap: 10 }}>
              <label htmlFor="photo" className="file-picker-button">
                {photo ? "사진 변경" : "사진 선택"}
              </label>
              <input id="photo" type="file" accept="image/*" onChange={handlePhotoChange} className="visually-hidden-input" />
              {photo && <span className="muted">{photo.name}</span>}
            </div>
          </div>
        )}
        {error && <div className="error-banner">{error}</div>}
        <button type="submit" className="primary" disabled={loading}>
          {loading ? "처리 중..." : mode === "signup" ? "회원가입" : "로그인"}
        </button>
      </form>
    </div>
  );
}
