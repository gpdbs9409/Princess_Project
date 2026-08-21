import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError } from "../api/client";
import { resetPassword } from "../api/endpoints";

const MIN_PASSWORD_LENGTH = 4;

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < MIN_PASSWORD_LENGTH) {
      setError(`비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 해요.`);
      return;
    }
    if (password !== confirmPassword) {
      setError("비밀번호가 일치하지 않아요.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await resetPassword(token, password);
      setDone(true);
      setTimeout(() => navigate("/login"), 2000);
    } catch (err) {
      if (err instanceof ApiError && err.code === "TOKEN_EXPIRED") {
        setError("링크가 만료됐어요. 비밀번호 찾기를 다시 요청해주세요.");
      } else if (err instanceof ApiError && err.code === "TOKEN_ALREADY_USED") {
        setError("이미 사용된 링크예요. 비밀번호 찾기를 다시 요청해주세요.");
      } else if (err instanceof ApiError && err.code === "TOKEN_INVALID") {
        setError("유효하지 않은 링크예요. 비밀번호 찾기를 다시 요청해주세요.");
      } else {
        setError("비밀번호 재설정에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="container" style={{ maxWidth: 420, paddingTop: 96 }}>
        <div className="card stack" style={{ gap: 12 }}>
          <p>유효하지 않은 링크예요.</p>
          <Link to="/forgot-password" className="link">비밀번호 찾기 다시 요청하기</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: 420, paddingTop: 96 }}>
      <div className="stack" style={{ marginBottom: 28, textAlign: "center" }}>
        <span className="eyebrow">Princess Diary</span>
        <h1 style={{ fontSize: 26 }}>새 비밀번호 설정</h1>
      </div>

      {done ? (
        <div className="card stack" style={{ gap: 12 }}>
          <p>비밀번호가 변경됐어요. 로그인 화면으로 이동할게요...</p>
        </div>
      ) : (
        <form className="card stack" onSubmit={handleSubmit}>
          <div className="stack" style={{ gap: 6 }}>
            <label htmlFor="password">새 비밀번호</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="새 비밀번호"
              autoFocus
            />
          </div>
          <div className="stack" style={{ gap: 6 }}>
            <label htmlFor="confirm-password">새 비밀번호 확인</label>
            <input
              id="confirm-password"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="새 비밀번호 확인"
            />
          </div>
          {error && <div className="error-banner">{error}</div>}
          <button type="submit" className="primary" disabled={loading}>
            {loading ? "변경 중..." : "비밀번호 변경"}
          </button>
        </form>
      )}
    </div>
  );
}
