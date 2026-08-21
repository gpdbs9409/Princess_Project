// 온보딩 브릿지 마지막 단계(엔딩 공주 갤러리)에 쓰이는 자리표시 데이터.
//
// TODO(쥐콩이): 아래 7개는 자본별 예시 1개씩만 채운 자리표시용이에요. "심리" 항목만
// "카톡프린세스 다이어리 브릿지 화면_v0.3_260816.pptx"에 있던 실제 예시 카피고,
// 나머지 6개는 레이아웃 확인용으로 제가 임의로 채운 (예시) 텍스트예요 - 절대 최종 카피 아님.
// 실제 엔딩 공주 카피(기획서 기준 최종 13종)와 전용 일러스트가 나오면 이 배열을 통째로
// 교체해주세요. 지금은 이미지도 실제 일러스트 대신 자본 아이콘(/capitals/*.png)을 쓰고 있어요.
export interface OnboardingEnding {
  id: string;
  capitalKey: "psychology" | "culture" | "knowledge" | "economy" | "physical" | "language" | "symbol";
  capitalLabel: string;
  title: string;
  description: string;
}

export const ONBOARDING_ENDINGS: OnboardingEnding[] = [
  {
    id: "psychology",
    capitalKey: "psychology",
    capitalLabel: "심리",
    title: "해탈의 경지에 오른 멘탈갑 공주",
    description:
      "어떤 시련과 비난에도 흔들리지 않는 강철 멘탈의 소유자. 내면의 평화가 우주급이라 주변의 온갖 스트레스를 증발시키는 인간 힐러이자 명상가.",
  },
  {
    id: "culture",
    capitalKey: "culture",
    capitalLabel: "문화",
    title: "(예시) 취향으로 세상을 읽는 안목 공주",
    description: "TODO(쥐콩이): 문화 자본 엔딩 타이틀/설명 카피 확정 필요.",
  },
  {
    id: "knowledge",
    capitalKey: "knowledge",
    capitalLabel: "지식",
    title: "(예시) 질문 하나로 판을 뒤집는 브레인 공주",
    description: "TODO(쥐콩이): 지식 자본 엔딩 타이틀/설명 카피 확정 필요.",
  },
  {
    id: "economy",
    capitalKey: "economy",
    capitalLabel: "경제",
    title: "(예시) 숫자로 미래를 설계하는 자산가 공주",
    description: "TODO(쥐콩이): 경제 자본 엔딩 타이틀/설명 카피 확정 필요.",
  },
  {
    id: "physical",
    capitalKey: "physical",
    capitalLabel: "신체",
    title: "(예시) 무너지지 않는 체력으로 판을 버티는 공주",
    description: "TODO(쥐콩이): 신체 자본 엔딩 타이틀/설명 카피 확정 필요.",
  },
  {
    id: "language",
    capitalKey: "language",
    capitalLabel: "언어",
    title: "(예시) 말 한마디로 분위기를 바꾸는 공주",
    description: "TODO(쥐콩이): 언어 자본 엔딩 타이틀/설명 카피 확정 필요.",
  },
  {
    id: "symbol",
    capitalKey: "symbol",
    capitalLabel: "상징",
    title: "(예시) 존재만으로 아우라가 되는 공주",
    description: "TODO(쥐콩이): 상징 자본 엔딩 타이틀/설명 카피 확정 필요.",
  },
];
