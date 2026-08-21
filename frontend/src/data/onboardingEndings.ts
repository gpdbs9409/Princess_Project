// 온보딩 브릿지 마지막 단계(엔딩 공주 갤러리)에 쓰이는 데이터.
//
// 카피 확정 (2026-08-21): 자본 7종 x 엔딩 캐릭터 1개씩, 총 7개로 최종 확정됐다.
// 전용 일러스트는 아직 없어서 지금은 자본 아이콘(/capitals/*.png)을 임시로 쓰고 있다.
// 실제 사진이 오면 frontend/public/endings/{capitalKey}.png 로 넣고
// OnboardingBridge.tsx의 이미지 src만 /capitals/ → /endings/ 로 바꿔주면 된다.
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
    title: "트렌드를 쥐락펴락하는 살롱의 지배자 예술가 공주",
    description:
      "그녀가 소비하는 모든 것이 곧 트렌드가 되며, 방구석 인디 감성부터 하이엔드 파인 아트까지 섭렵한 이 시대의 독보적인 뮤즈.",
  },
  {
    id: "knowledge",
    capitalKey: "knowledge",
    capitalLabel: "지식",
    title: "걸어다니는 알쓸신잡 학자 공주",
    description:
      "방대한 빅데이터를 머리에 탑재한 팩트 폭격기. 논리와 학문으로 세상을 분석하고 제도를 뜯어고치는 천재 서기관.",
  },
  {
    id: "economy",
    capitalKey: "economy",
    capitalLabel: "경제",
    title: "막대한 자본을 축적한 대지주 공주",
    description:
      "왕관 대신 빌딩 등기부등본을 쥐고 있는 자본주의의 정점. 돈이 돈을 낳는 완벽한 시스템을 구축하여, 숨만 쉬어도 자산이 복사되는 풍요의 화신.",
  },
  {
    id: "physical",
    capitalKey: "physical",
    capitalLabel: "신체",
    title: "근손실을 용납하지 않는 세계관 최강 전사 공주",
    description:
      "화려한 드레스 속에 압도적인 피지컬과 근육을 숨겨둔 공주. 위기 상황이 오면 왕자를 기다리지 않고 몬스터를 맨손으로 때려잡는 걸어다니는 인간 병기.",
  },
  {
    id: "language",
    capitalKey: "language",
    capitalLabel: "언어",
    title: "혀 하나로 제국을 들었다 놓는 천재 외교관 공주",
    description:
      "청산유수 같은 말재주로 적군마저 아군으로 만드는 협상의 신. 탁월한 스토리텔링과 화술로 상대의 마음과 지갑을 모두 여는 마술사.",
  },
  {
    id: "symbol",
    capitalKey: "symbol",
    capitalLabel: "상징",
    title: "숨만 쉬어도 실검 1위 스포트라이트 인플루언서 공주",
    description:
      "전통적인 왕실의 권위 대신 대중의 절대적인 지지와 평판을 먹고 사는 셀럽. 그녀의 손짓 하나, 말 한마디로 사회적 트렌드가 뒤바뀌는 현대판 여왕.",
  },
];
