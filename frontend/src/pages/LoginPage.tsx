import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login, updateStatFocus } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { StatFocusForm } from "../components/StatFocusForm";
import type { UserResponse } from "../api/types";

export function LoginPage() {
  const { signIn, updateUser } = useAuth();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingUser, setPendingUser] = useState<UserResponse | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const res = await login(nickname.trim());
      signIn(res.token, res.user);
      if (Object.keys(res.user.statFocus).length === 0) {
        setPendingUser(res.user);
      } else {
        navigate("/dashboard");
      }
    } catch {
      setError("로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  if (pendingUser) {
    return (
      <div className="container" style={{ maxWidth: 480, paddingTop: 64 }}>
        <div className="stack" style={{ marginBottom: 24 }}>
          <span className="eyebrow">2 / 2</span>
          <h1 style={{ fontSize: 28 }}>스탯 비중을 설정해주세요</h1>
          <p className="muted">앞으로의 기록이 어떤 스탯에 더 반영될지 정하는 단계예요.</p>
        </div>
        <div className="card">
          <StatFocusForm
            initial={{}}
            submitLabel="설정 완료"
            onSubmit={async (stats) => {
              const updated = await updateStatFocus(pendingUser.id, stats);
              updateUser(updated);
              navigate("/dashboard");
            }}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: 420, paddingTop: 96 }}>
      <div className="stack" style={{ marginBottom: 28, textAlign: "center" }}>
        <span className="eyebrow">Princess Project</span>
        <h1 style={{ fontSize: 30 }}>오늘의 나를 기록해요</h1>
        <p className="muted">닉네임만 입력하면 시작할 수 있어요.</p>
      </div>
      <form className="card stack" onSubmit={handleLogin}>
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
        {error && <div className="error-banner">{error}</div>}
        <button type="submit" className="primary" disabled={loading}>
          {loading ? "로그인 중..." : "시작하기"}
        </button>
      </form>
    </div>
  );
}
