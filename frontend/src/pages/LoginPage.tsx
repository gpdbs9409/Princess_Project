import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../api/client";
import { getActiveProject, login, signup, updateProfileImage } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";

type Mode = "login" | "signup";

// 약관 원문은 아직 확정 전 (2026-08-21: 쥐콩이가 문구 전달 예정, 받으면 TERMS_PLACEHOLDER_TEXT
// 자리에 실제 약관 내용으로 교체). 지금은 자리표시 문구만 넣어 체크박스 UI/검증 로직을 먼저 붙인다.
type TermItem = {
  id: string;
  label: string;
  required: boolean;
};

const TERMS: TermItem[] = [
  { id: "service", label: "이용약관 동의", required: true },
  { id: "privacy", label: "개인정보 수집 및 이용 동의", required: true },
  { id: "marketing", label: "마케팅 정보 수신 동의 (선택)", required: false },
];

const TERMS_PLACEHOLDER_TEXT = "약관 추후 변경 예정입니다.";

export function LoginPage() {
  const { signIn, updateUser } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>("login");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState<string | null>(null);
  const [agreements, setAgreements] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(TERMS.map((term) => [term.id, false]))
  );
  const [expandedTermId, setExpandedTermId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const allTermsAgreed = TERMS.every((term) => agreements[term.id]);
  const requiredTermsAgreed = TERMS.filter((term) => term.required).every(
    (term) => agreements[term.id]
  );

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

  const toggleAllTerms = (checked: boolean) => {
    setAgreements(Object.fromEntries(TERMS.map((term) => [term.id, checked])));
  };

  const toggleTerm = (id: string, checked: boolean) => {
    setAgreements((prev) => ({ ...prev, [id]: checked }));
  };

  const toggleTermPreview = (id: string) => {
    setExpandedTermId((prev) => (prev === id ? null : id));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname.trim() || !password) return;
    if (mode === "signup" && !requiredTermsAgreed) {
      setError("필수 약관에 모두 동의해주세요.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res =
        mode === "signup"
          ? await signup(nickname.trim(), password, email.trim())
          : await login(nickname.trim(), password);
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
      } else if (err instanceof ApiError && mode === "signup" && err.code === "EMAIL_TAKEN") {
        setError("이미 사용 중인 이메일이에요. 다른 이메일을 입력해주세요.");
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
          <div className="stack" style={{ gap: 6 }}>
            <label htmlFor="email">이메일 (선택)</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="비밀번호를 잊었을 때 필요해요"
            />
          </div>
        )}
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
        {mode === "signup" && (
          <div className="terms-block">
            <label className="terms-row terms-row-all">
              <input
                type="checkbox"
                checked={allTermsAgreed}
                onChange={(e) => toggleAllTerms(e.target.checked)}
              />
              <span>약관 전체 동의</span>
            </label>
            <div className="terms-divider" />
            <div className="stack" style={{ gap: 2 }}>
              {TERMS.map((term) => (
                <div key={term.id} className="terms-row">
                  <label className="terms-checkbox-label" htmlFor={`term-${term.id}`}>
                    <input
                      id={`term-${term.id}`}
                      type="checkbox"
                      checked={agreements[term.id]}
                      onChange={(e) => toggleTerm(term.id, e.target.checked)}
                    />
                    <span>
                      <span className={term.required ? "terms-tag required" : "terms-tag optional"}>
                        {term.required ? "필수" : "선택"}
                      </span>{" "}
                      {term.label}
                    </span>
                  </label>
                  <button
                    type="button"
                    className="terms-view-toggle"
                    onClick={() => toggleTermPreview(term.id)}
                  >
                    {expandedTermId === term.id ? "닫기" : "보기"}
                  </button>
                </div>
              ))}
            </div>
            {expandedTermId && <p className="muted terms-content">{TERMS_PLACEHOLDER_TEXT}</p>}
          </div>
        )}
        {error && <div className="error-banner">{error}</div>}
        <button type="submit" className="primary" disabled={loading || (mode === "signup" && !requiredTermsAgreed)}>
          {loading ? "처리 중..." : mode === "signup" ? "회원가입" : "로그인"}
        </button>
        {mode === "login" && (
          <Link to="/forgot-password" className="link" style={{ textAlign: "center" }}>
            비밀번호를 잊으셨나요?
          </Link>
        )}
      </form>
    </div>
  );
}
