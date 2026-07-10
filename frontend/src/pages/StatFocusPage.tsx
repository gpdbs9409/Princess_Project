import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getActiveProject, getCatalog, replaceSelections } from "../api/endpoints";
import { SelectionWizard } from "../components/SelectionWizard";
import type { CatalogGoal, ProjectResponse } from "../api/types";

export function StatFocusPage() {
  const navigate = useNavigate();
  const [catalog, setCatalog] = useState<CatalogGoal[] | null>(null);
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getCatalog(), getActiveProject()])
      .then(([catalogData, projectData]) => {
        setCatalog(catalogData);
        setProject(projectData);
      })
      .catch(() => setError("데이터를 불러오지 못했습니다."));
  }, []);

  return (
    <div className="container">
      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">설정</span>
        <h1 style={{ fontSize: 26 }}>습관자본 · 미션 수정</h1>
        <p className="muted">저장하면 기존 선택을 전부 교체합니다.</p>
      </div>
      {error && <div className="error-banner">{error}</div>}
      {catalog && project && (
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
