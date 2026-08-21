import { useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../api/client";
import { forgotPassword } from "../api/endpoints";

export function ForgotPasswordPage() {
  const [nickname, setNickname] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname.trim()) return;
    setLoading(true);
    setError(null);
    try {
      await forgotPassword(nickname.trim());
      setSent(true);
    } catch (err) {
      if (err instanceof ApiError && err.code === "NICKNAME_NOT_FOUND") {
        setError("존재하지 않는 닉네임이에요.");
      } else if (err instanceof ApiError && err.code === "EMAIL_NOT_SET") {
        setError("이 계정에는 등록된 이메일이 없어요. 마이페이지에서 먼저 이메일을 등록해주세요.");
      } else {
        setError("요청에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: 420, paddingTop: 96 }}>
      <div className="stack" style={{ marginBottom: 28, textAlign: "center" }}>
        <span className="eyebrow">Princess Diary</span>
        <h1 style={{ fontSize: 26 }}>비밀번호 찾기</h1>
      </div>

      {sent ? (
        <div className="card stack" style={{ gap: 12 }}>
          <p>등록된 이메일로 비밀번호 재설정 링크를 보냈어요. 메일함(스팸함 포함)을 확인해주세요.</p>
          <p className="muted">링크는 30분 동안만 유효해요.</p>
          <Link to="/login" className="link">로그인으로 돌아가기</Link>
        </div>
      ) : (
        <form className="card stack" onSubmit={handleSubmit}>
          <div className="stack" style={{ gap: 6 }}>
            <label htmlFor="nickname">닉네임</label>
            <input
              id="nickname"
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="가입할 때 쓴 닉네임"
              autoFocus
            />
          </div>
          {error && <div className="error-banner">{error}</div>}
          <button type="submit" className="primary" disabled={loading}>
            {loading ? "전송 중..." : "재설정 메일 보내기"}
          </button>
          <Link to="/login" className="link" style={{ textAlign: "center" }}>
            로그인으로 돌아가기
          </Link>
        </form>
      )}
    </div>
  );
}
