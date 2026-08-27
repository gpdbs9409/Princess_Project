// 카카오톡/인스타 초대 링크로 처음 들어온 방문자에게 보여주는 스토리텔링 브릿지 팝업 관련 설정.
// 별도 URL 없이 기존 로그인 화면(/login) 위에 오버레이로 뜬다.
// 기획 근거: 2026-08-13 온보딩 전략 회의 + "카톡프린세스 다이어리 브릿지 화면_v0.3_260816.pptx"

export type OnboardingStep = "video" | "climax" | "gallery";

const PROGRESS_KEY = "princess_onboarding_progress_v1";
const DISMISSED_KEY = "princess_onboarding_dismissed_v1";

// 회의/기획서 기준: 9/4까지는 팝업 노출, 9/5부터는 완전히 숨김 처리.
// KST 자정 기준으로 고정. (참고: 9/4 테스트 완료 목표는 회의록 기준, 실제 컷오프는 9/5 0시)
export const ONBOARDING_HIDE_AFTER = new Date("2026-09-05T00:00:00+09:00");

export function isOnboardingWindowOpen(now: Date = new Date()): boolean {
  return now.getTime() < ONBOARDING_HIDE_AFTER.getTime();
}

export interface OnboardingProgress {
  step: OnboardingStep;
  galleryIndex: number;
}

const DEFAULT_PROGRESS: OnboardingProgress = { step: "video", galleryIndex: 0 };

// 재접속 시 이전에 보던 지점부터 이어볼 수 있도록 진행 상태를 기억해둔다 (2026-08-13 회의록:
// "사용자가 도중에 이탈했다가 다시 접속해도 이전에 보던 페이지부터 이어볼 수 있도록").
export function loadOnboardingProgress(): OnboardingProgress {
  try {
    const raw = window.localStorage.getItem(PROGRESS_KEY);
    if (!raw) return { ...DEFAULT_PROGRESS };
    const parsed = JSON.parse(raw) as Partial<OnboardingProgress>;
    if (parsed.step !== "video" && parsed.step !== "climax" && parsed.step !== "gallery") {
      return { ...DEFAULT_PROGRESS };
    }
    return {
      step: parsed.step,
      galleryIndex: typeof parsed.galleryIndex === "number" ? parsed.galleryIndex : 0,
    };
  } catch {
    return { ...DEFAULT_PROGRESS };
  }
}

export function saveOnboardingProgress(progress: OnboardingProgress) {
  try {
    window.localStorage.setItem(PROGRESS_KEY, JSON.stringify(progress));
  } catch {
    // 프라이빗 모드 등 localStorage 접근 불가 환경 - 조용히 무시 (다음 진입 시 처음부터 다시 보여줌)
  }
}

export function isOnboardingDismissed(): boolean {
  try {
    return window.localStorage.getItem(DISMISSED_KEY) === "true";
  } catch {
    return false;
  }
}

export function dismissOnboarding() {
  try {
    window.localStorage.setItem(DISMISSED_KEY, "true");
  } catch {
    // Storage can be unavailable in private mode; the current component still closes.
  }
}
