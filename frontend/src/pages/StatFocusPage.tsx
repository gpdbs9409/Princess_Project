import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { getActiveProject, getCatalog, replaceSelections } from "../api/endpoints";
import { SelectionWizard } from "../components/SelectionWizard";
import { ProjectReadOnlyView } from "../components/ProjectReadOnlyView";
import type { CatalogGoal, ProjectResponse } from "../api/types";

export function StatFocusPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [catalog, setCatalog] = useState<CatalogGoal[] | null>(null);
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showWelcome, setShowWelcome] = useState(() => Boolean((location.state as { welcome?: boolean } | null)?.welcome));

  useEffect(() => {
    Promise.all([getCatalog(), getActiveProject()])
      .then(([catalogData, projectData]) => {
        setCatalog(catalogData);
        setProject(projectData);
      })
      .catch(() => setError("데이터를 불러오지 못했습니다."));
  }, []);

  const alreadySet = !!project && project.goals.length > 0;

  if (alreadySet) return <Navigate to="/my-page" replace />;

  return (
    <div className="container">
      {showWelcome && (
        <div className="modal-overlay">
          <div className="modal-card">
            <h2 style={{ fontSize: 22 }}>환영합니다!</h2>
            <p className="muted">자본스탯(나만의 아비투스)과 미션을 설정하고 오늘부터 프린세스가 되어보세요.</p>
            <button className="primary" onClick={() => setShowWelcome(false)}>
              시작하기
            </button>
          </div>
        </div>
      )}

      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">{alreadySet ? "나의 아비투스" : "설정"}</span>
        <h1 style={{ fontSize: 26 }}>{alreadySet ? "나의 아비투스" : "아비투스 · 미션 설정"}</h1>
        <p className="muted">
          {alreadySet
            ? "나의 아비투스와 미션은 최초 설정 후에는 수정할 수 없어요. 수정이 필요한 경우에는 운영진에 문의해주세요."
            : "아비투스와 미션은 처음 한 번만 설정할 수 있어요. 신중하게 선택해주세요."}
        </p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {project && alreadySet && <ProjectReadOnlyView project={project} />}

      {catalog && project && !alreadySet && (
        <SelectionWizard
          catalog={catalog}
          initialProject={project}
          submitLabel="저장하기"
          onSubmit={async (request) => {
            await replaceSelections(request);
            navigate("/dashboard");
          }}
        />
      )}
    </div>
  );
}
