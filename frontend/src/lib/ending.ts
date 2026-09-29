import { useCallback, useEffect, useState } from "react";
import { getEndingStatus } from "../api/endpoints";
import type { EndingStatusResponse, GoalTypeCode } from "../api/types";

// ---- 카드 콘텐츠 (35종 = 공주 유형 7 × 결말 단계 5) ----

export const ENDING_STAGE_NAMES: Record<number, string> = {
  1: "각성한 조연",
  2: "영애",
  3: "공녀",
  4: "대공녀",
  5: "공주",
};

// 화면설계서 11p 매핑표. 카드 이미지 자체에 타이틀/설명이 들어 있어서, 이 값은 대체 텍스트와
// 이미지 로드 실패 시 표시용으로만 쓴다.
export const ENDING_CARD_TITLES: Record<GoalTypeCode, [string, string, string, string, string]> = {
  PSYCHOLOGY: ["무덤덤 조연", "포커페이스 영애", "강철멘탈 공녀", "타격감 Zero 대공녀", "해탈의 공주"],
  CULTURE: ["유행에 눈뜬 조연", "셀럽 지망 영애", "사교계 인싸 공녀", "트렌드세터 대공녀", "예술가 공주"],
  KNOWLEDGE: ["독서광 조연", "TMI 수집가 영애", "척척학사 공녀", "제국 브레인 대공녀", "학자 공주"],
  ECONOMY: ["짠테크 조연", "시드머니 수집가 영애", "자산 자가복제 공녀", "등기부등본 부자 대공녀", "대지주 공주"],
  PHYSICAL: ["득근에 눈뜬 조연", "단백질 러버 영애", "3대 500 공녀", "걸어다니는 인간병기 대공녀", "전사 공주"],
  LANGUAGE: ["말문 터진 조연", "제국의 MC 영애", "현기증 유발 협상가 공녀", "제국 말발짱 대공녀", "외교관 공주"],
  SYMBOL: ["어그로 조연", "사교계 입문 영애", "사교계 인기녀 공녀", "셀럽 도전 대공녀", "인플루언서 공주"],
};

export function endingCardTitle(capital: GoalTypeCode, stage: number): string {
  const titles = ENDING_CARD_TITLES[capital];
  return titles ? titles[Math.min(5, Math.max(1, stage)) - 1] : "";
}

/** frontend/public/endings/cards/{capital}-{stage}.png (폭 330px, 원본 시안) */
export function endingCardImage(capital: GoalTypeCode, stage: number): string {
  return `/endings/cards/${capital.toLowerCase()}-${stage}.png`;
}

export const CARD_WIDTH = 330;
/** 35종 중 가장 긴 카드(인플루언서 공주 등) 기준으로 높이 고정 */
export const CARD_HEIGHT = 564;

export const OPEN_CHAT_URL = "https://open.kakao.com/o/swJ7TIKi";

// ---- 관리자 미리보기 (공개일 전 dev 검증용) ----
// URL에 ?endingPreview=1 을 붙이면 이 브라우저 탭 세션 동안 엔딩 API를 preview=true로 부른다.
// 서버가 관리자에게만 적용하므로 일반 참가자에게는 아무 효과가 없다. ?endingPreview=0 으로 해제.
const PREVIEW_KEY = "princess_ending_preview";

export function syncEndingPreviewFromUrl(search: string) {
  const value = new URLSearchParams(search).get("endingPreview");
  try {
    if (value === "1") sessionStorage.setItem(PREVIEW_KEY, "1");
    if (value === "0") sessionStorage.removeItem(PREVIEW_KEY);
  } catch {
    // storage unavailable - preview simply stays off
  }
}

export function isEndingPreview(): boolean {
  try {
    return sessionStorage.getItem(PREVIEW_KEY) === "1";
  } catch {
    return false;
  }
}

// ---- 날짜 (KST) ----

export function kstDateKey(date: Date = new Date()): string {
  // en-CA 로케일은 YYYY-MM-DD 형식
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(date);
}

// ---- 확인/노출 기록 (브라우저 단위) ----

function storageKey(userId: number | undefined, name: string) {
  return `princess_ending_${name}_${userId ?? "anon"}`;
}

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string) {
  try {
    localStorage.setItem(key, value);
  } catch {
    // ignore
  }
}

export const ENDING_VISITED_EVENT = "princess-ending-visited";

/** 엔딩 페이지 진입 기록 - 공개 팝업(최초 확인 여부)과 N 뱃지(오늘 진입 여부)에 쓴다. */
export function markEndingVisited(userId: number | undefined) {
  write(storageKey(userId, "viewed"), "1");
  write(storageKey(userId, "visited_on"), kstDateKey());
  window.dispatchEvent(new Event(ENDING_VISITED_EVENT));
}

export function hasViewedEnding(userId: number | undefined): boolean {
  return read(storageKey(userId, "viewed")) === "1";
}

/** N 뱃지: 매일 첫 진입 전까지 노출, 그날 한 번 들어가면 해제. */
export function visitedEndingToday(userId: number | undefined): boolean {
  return read(storageKey(userId, "visited_on")) === kstDateKey();
}

export function dismissPopupToday(userId: number | undefined) {
  write(storageKey(userId, "popup_dismissed_on"), kstDateKey());
}

export function popupDismissedToday(userId: number | undefined): boolean {
  return read(storageKey(userId, "popup_dismissed_on")) === kstDateKey();
}

// ---- 서버 기준 공개 상태 ----

let statusCache: { preview: boolean; promise: Promise<EndingStatusResponse> } | null = null;

export function fetchEndingStatus(force = false): Promise<EndingStatusResponse> {
  const preview = isEndingPreview();
  if (!force && statusCache && statusCache.preview === preview) return statusCache.promise;
  const promise = getEndingStatus(preview);
  statusCache = { preview, promise };
  promise.catch(() => {
    if (statusCache?.promise === promise) statusCache = null;
  });
  return promise;
}

export function useEndingStatus(enabled: boolean) {
  const [status, setStatus] = useState<EndingStatusResponse | null>(null);
  const [error, setError] = useState(false);

  const load = useCallback((force = false) => {
    setError(false);
    fetchEndingStatus(force)
      .then(setStatus)
      .catch(() => setError(true));
  }, []);

  useEffect(() => {
    if (enabled) load();
  }, [enabled, load]);

  return { status, error, reload: () => load(true) };
}

// ---- 환경 ----

export function isMobileDevice(): boolean {
  return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
}

/** 카카오톡/인스타그램/네이버 등 인앱 브라우저 - 파일 다운로드가 막혀 있는 경우가 많다. */
export function isInAppBrowser(): boolean {
  return /KAKAOTALK|Instagram|FBAN|FBAV|NAVER|Line\/|DaumApps|everytimeApp|; wv\)/i.test(navigator.userAgent);
}
