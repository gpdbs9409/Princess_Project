import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError } from "../api/client";
import { getActiveProject, getCatalog, login, replaceSelections } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { SelectionWizard } from "../components/SelectionWizard";
import type { CatalogGoal } from "../api/types";

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [catalog, setCatalog] = useState<CatalogGoal[] | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname.trim() || !password) return;
    setLoading(true);
    setError(null);
    try {
      const res = await login(nickname.trim(), password);
      signIn(res.token, res.user);

      const project = await getActiveProject();
      if (project.goals.length === 0) {
        const catalogData = await getCatalog();
        setCatalog(catalogData);
      } else {
        navigate("/dashboard");
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError("닉네임 또는 비밀번호가 올바르지 않습니다.");
      } else {
        setError("로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setLoading(false);
    }
  };

  if (catalog) {
    return (
      <div className="container" style={{ maxWidth: 560, paddingTop: 48 }}>
        <div className="stack" style={{ marginBottom: 24 }}>
          <span className="eyebrow">온보딩</span>
          <h1 style={{ fontSize: 26 }}>어떤 습관을 키워볼까요?</h1>
          <p className="muted">습관자본과 행동양식, 그리고 매일 인증할 미션을 골라주세요.</p>
        </div>
        <SelectionWizard
          catalog={catalog}
          submitLabel="설정 완료"
          onSubmit={async (request) => {
            await replaceSelections(request);
            navigate("/dashboard");
          }}
        />
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: 420, paddingTop: 96 }}>
      <div className="stack" style={{ marginBottom: 28, textAlign: "center" }}>
        <span className="eyebrow">Princess Project</span>
        <h1 style={{ fontSize: 30 }}>오늘의 나를 기록해요</h1>
        <p className="muted">닉네임과 비밀번호로 로그인하세요. 처음 보는 닉네임이면 그 비밀번호로 계정이 만들어져요.</p>
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
        {error && <div className="error-banner">{error}</div>}
        <button type="submit" className="primary" disabled={loading}>
          {loading ? "로그인 중..." : "시작하기"}
        </button>
      </form>
    </div>
  );
}
