import { WeeklyRetrospectiveSection } from "../components/CommonTasksCard";

// 주간 회고는 공통 과제 3종 중 하나지만, 나머지 둘(독서/공부)과 달리 주 1회만 작성하면 되는
// 과제다. 매일 도는 /record의 습관 목록에 섞여 있으면 매일 스쳐 지나가기 쉬워서, 상단 메뉴
// 전용 화면으로 분리했다 (2026-08-21 요청: "주간회고 공통과제는 따로 상단 내비게이션에 분리해서
// 주 1회만 보고 트래킹 가능하게"). 실제 폼/저장 로직은 CommonTasksCard.tsx의
// WeeklyRetrospectiveSection을 그대로 재사용한다 - 로직을 복제하지 않기 위해서다.
export function WeeklyRetrospectivePage() {
  return (
    <div className="container">
      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">Weekly</span>
        <h1 style={{ fontSize: 26 }}>주간 회고</h1>
        <p className="muted">한 주를 돌아보고 다음주의 계획을 세워요</p>
      </div>

      <WeeklyRetrospectiveSection />
    </div>
  );
}
