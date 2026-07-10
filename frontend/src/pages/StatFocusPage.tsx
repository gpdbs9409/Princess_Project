import { useNavigate } from "react-router-dom";
import { updateStatFocus } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { StatFocusForm } from "../components/StatFocusForm";

export function StatFocusPage() {
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();
  if (!user) return null;

  return (
    <div className="container">
      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">설정</span>
        <h1 style={{ fontSize: 26 }}>스탯 비중 수정</h1>
        <p className="muted">저장하면 기존 비중을 전부 교체합니다.</p>
      </div>
      <div className="card">
        <StatFocusForm
          initial={user.statFocus}
          submitLabel="저장하기"
          onSubmit={async (stats) => {
            const updated = await updateStatFocus(user.id, stats);
            updateUser(updated);
            navigate("/dashboard");
          }}
        />
      </div>
    </div>
  );
}
