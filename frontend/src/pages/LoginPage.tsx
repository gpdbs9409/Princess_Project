import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../api/client";
import {
  confirmEmailVerification,
  getActiveProject,
  login,
  requestEmailVerification,
  signup,
  updateProfileImage,
} from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";

type Mode = "login" | "signup";
type VerifyStep = "none" | "sent" | "verified";

export function LoginPage() {
  const { signIn, updateUser } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>("login");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 이메일 인증 (2026-08-26 요청: 이메일 선택 -> 필수, 인증에 성공해야만 회원가입 버튼이 풀린다).
  // verifiedToken은 이메일이 바뀌면 더 이상 그 이메일에 대해 유효하지 않으므로, 이메일을 다시
  // 수정하면 인증 상태를 처음(none)으로 되돌린다 - handleEmailChange에서 처리.
  const [verifyStep, setVerifyStep] = useState<VerifyStep>("none");
  const [verifyCode, setVerifyCode] = useState("");
  const [verifiedToken, setVerifiedToken] = useState<string | null>(null);
  const [verifySending, setVerifySending] = useState(false);
  const [verifyConfirming, setVerifyConfirming] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [verifyInfo, setVerifyInfo] = useState<string | null>(null);

  useEffect(() => {
    return () => {
      if (photoPreviewUrl) URL.revokeObjectURL(photoPreviewUrl);
    };
  }, [photoPreviewUrl]);

  const resetVerification = () => {
    setVerifyStep("none");
    setVerifiedToken(null);
    setVerifyCode("");
    setVerifyError(null);
    setVerifyInfo(null);
  };

  const switchMode = (next: Mode) => {
    setMode(next);
    setError(null);
    resetVerification();
  };

  const handleEmailChange = (value: string) => {
    setEmail(value);
    if (verifyStep !== "none") resetVerification();
  };

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setPhoto(file);
    setPhotoPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return file ? URL.createObjectURL(file) : null;
    });
  };

  const handleRequestVerification = async () => {
    if (!email.trim()) return;
    setVerifySending(true);
    setVerifyError(null);
    setVerifyInfo(null);
    try {
      await requestEmailVerification(email.trim());
      setVerifyStep("sent");
      setVerifyInfo("인증 코드를 보냈어요. 메일함(스팸함 포함)을 확인해주세요.");
    } catch {
      setVerifyError("인증 코드 발송에 실패했습니다. 이메일 주소를 확인하고 다시 시도해주세요.");
    } finally {
      setVerifySending(false);
    }
  };

  const handleConfirmVerification = async () => {
    if (verifyCode.length !== 6) return;
    setVerifyConfirming(true);
    setVerifyError(null);
    try {
      const res = await confirmEmailVerification(email.trim(), verifyCode);
      setVerifiedToken(res.verifiedToken);
      setVerifyStep("verified");
      setVerifyInfo(null);
    } catch (err) {
      if (err instanceof ApiError && err.code === "CODE_EXPIRED") {
        setVerifyError("인증 코드가 만료됐어요. 다시 발송해주세요.");
      } else if (err instanceof ApiError && err.code === "CODE_INVALID") {
        setVerifyError("인증 코드가 올바르지 않아요.");
      } else if (err instanceof ApiError && err.code === "CODE_NOT_REQUESTED") {
        setVerifyError("먼저 '인증하기'를 눌러 코드를 받아주세요.");
      } else {
        setVerifyError("인증에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setVerifyConfirming(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname.trim() || !password) return;
    if (mode === "signup" && (verifyStep !== "verified" || !verifiedToken)) return;
    setLoading(true);
    setError(null);
    try {
      const res =
        mode === "signup"
          ? await signup(nickname.trim(), password, email.trim(), verifiedToken as string)
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
      } else if (err instanceof ApiError && mode === "signup" && err.code === "EMAIL_NOT_VERIFIED") {
        setError("이메일 인증이 만료됐어요. 인증을 다시 진행해주세요.");
        resetVerification();
      } else {
        setError(mode === "signup" ? "회원가입에 실패했습니다. 잠시 후 다시 시도해주세요." : "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setLoading(false);
    }
  };

  const signupDisabled = loading || (mode === "signup" && verifyStep !== "verified");

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
            <label htmlFor="email">이메일</label>
            <div className="row" style={{ gap: 8 }}>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => handleEmailChange(e.target.value)}
                placeholder="이메일 인증에 사용돼요"
                disabled={verifyStep === "verified"}
                style={{ flex: 1 }}
              />
              <button
                type="button"
                className="ghost"
                onClick={handleRequestVerification}
                disabled={!email.trim() || verifySending || verifyStep === "verified"}
              >
                {verifyStep === "verified"
                  ? "인증완료"
                  : verifySending
                    ? "발송 중..."
                    : verifyStep === "sent"
                      ? "재발송"
                      : "인증하기"}
              </button>
            </div>
          </div>
        )}
        {mode === "signup" && verifyStep === "sent" && (
          <div className="stack" style={{ gap: 6 }}>
            <label htmlFor="verifyCode">인증 코드</label>
            <div className="row" style={{ gap: 8 }}>
              <input
                id="verifyCode"
                type="text"
                inputMode="numeric"
                maxLength={6}
                value={verifyCode}
                onChange={(e) => setVerifyCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                placeholder="6자리 코드"
                style={{ flex: 1 }}
              />
              <button
                type="button"
                className="ghost"
                onClick={handleConfirmVerification}
                disabled={verifyCode.length !== 6 || verifyConfirming}
              >
                {verifyConfirming ? "확인 중..." : "확인"}
              </button>
            </div>
            {verifyInfo && <p className="muted">{verifyInfo}</p>}
            {verifyError && <div className="error-banner">{verifyError}</div>}
          </div>
        )}
        {mode === "signup" && verifyStep === "verified" && (
          <p className="muted">이메일 인증을 완료했어요.</p>
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
        {error && <div className="error-banner">{error}</div>}
        <button type="submit" className="primary" disabled={signupDisabled}>
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
