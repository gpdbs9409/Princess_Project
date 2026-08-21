import { useEffect, useRef, useState, type PointerEvent } from "react";
import {
  isOnboardingWindowOpen,
  loadOnboardingProgress,
  saveOnboardingProgress,
  type OnboardingStep,
} from "../lib/onboarding";
import { ONBOARDING_ENDINGS } from "../data/onboardingEndings";

// 카카오톡/인스타 초대 링크로 들어온 신규 방문자에게 보여주는 스토리텔링 브릿지 팝업.
// 별도 URL 없이 로그인 화면 위에 오버레이로 뜨고, 닫으면 로그인/회원가입 화면이 그대로 드러난다.
// 기획: 2026-08-13 온보딩 전략 회의 + "카톡프린세스 다이어리 브릿지 화면_v0.3_260816.pptx"
//
// 아직 준비되지 않은 리소스 (자리표시자로 대체, 실제 파일이 생기면 경로만 바꾸면 됨):
//  - /onboarding/bridge-video.mp4, /onboarding/video-poster.png
//    → "지하철에서 다이어리를 발견 ~ 라이벌의 도발까지" 영상 (쥐콩이 제작 예정, 수동 재생)
//  - /onboarding/climax-freeze.png → 클라이맥스 프리즈 프레임 이미지
//  - 엔딩 공주 최종 카피/일러스트 → data/onboardingEndings.ts 의 TODO 참고
//
// 노출 조건 및 재접속 시 이어보기 로직은 lib/onboarding.ts 참고.
export function OnboardingBridge() {
  const [dismissed, setDismissed] = useState(false);
  const [step, setStep] = useState<OnboardingStep>("video");
  const [galleryIndex, setGalleryIndex] = useState(0);
  const [videoMissing, setVideoMissing] = useState(false);
  const dragStartX = useRef<number | null>(null);
  const hasMounted = useRef(false);

  // 진행 상태는 처음 마운트될 때 한 번만 불러온다. 이후 step/galleryIndex가 바뀔 때마다 저장한다.
  useEffect(() => {
    const progress = loadOnboardingProgress();
    setStep(progress.step);
    setGalleryIndex(progress.galleryIndex);
  }, []);

  useEffect(() => {
    if (!hasMounted.current) {
      // 방금 불러온 초기값을 다시 저장하는 불필요한 쓰기를 건너뛴다.
      hasMounted.current = true;
      return;
    }
    saveOnboardingProgress({ step, galleryIndex });
  }, [step, galleryIndex]);

  if (!isOnboardingWindowOpen() || dismissed) return null;

  const goToClimax = () => setStep("climax");
  const goBackToVideo = () => {
    setStep("video");
    setVideoMissing(false);
  };
  const goToGallery = () => {
    setStep("gallery");
    setGalleryIndex(0);
  };

  const lastIndex = ONBOARDING_ENDINGS.length - 1;
  const isLastCard = galleryIndex >= lastIndex;
  const currentEnding = ONBOARDING_ENDINGS[Math.min(galleryIndex, lastIndex)];

  const showPrev = () => setGalleryIndex((i) => Math.max(0, i - 1));
  const showNext = () => setGalleryIndex((i) => Math.min(lastIndex, i + 1));

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    dragStartX.current = e.clientX;
  };
  const handlePointerUp = (e: PointerEvent<HTMLDivElement>) => {
    if (dragStartX.current == null) return;
    const delta = e.clientX - dragStartX.current;
    dragStartX.current = null;
    const SWIPE_THRESHOLD = 40;
    if (delta > SWIPE_THRESHOLD) showPrev();
    else if (delta < -SWIPE_THRESHOLD) showNext();
  };

  return (
    <div className="onboarding-overlay" role="dialog" aria-modal="true" aria-label="프린세스 다이어리 소개">
      <div className="onboarding-card">
        <button type="button" className="onboarding-close" onClick={() => setDismissed(true)} aria-label="닫기">
          ×
        </button>

        {step === "video" && (
          <div className="onboarding-video-stage">
            {!videoMissing ? (
              <video
                className="onboarding-video"
                controls
                playsInline
                poster="/onboarding/video-poster.png"
                onEnded={goToClimax}
                onError={() => setVideoMissing(true)}
              >
                <source src="/onboarding/bridge-video.mp4" type="video/mp4" />
              </video>
            ) : (
              <div className="onboarding-video-fallback">
                <p style={{ fontWeight: 700 }}>영상을 준비 중이에요</p>
                <p className="muted">지하철에서 다이어리를 발견한 순간부터, 라이벌의 도발까지.</p>
              </div>
            )}
            <button type="button" className="ghost onboarding-skip" onClick={goToClimax}>
              {videoMissing ? "다음으로" : "건너뛰기"}
            </button>
          </div>
        )}

        {step === "climax" && (
          <div className="onboarding-climax">
            <div className="onboarding-climax-image" aria-hidden="true" />
            <p className="onboarding-climax-countdown">D-30 · 봄의 무도회</p>
            <p className="onboarding-climax-copy">
              거울 속 나를 바라본다. 흐트러진 머리를 깔끔하게 묶어 올리며, 화면을 정면으로 응시한다.
            </p>
            <div className="onboarding-climax-actions">
              <button type="button" className="ghost" onClick={goBackToVideo}>
                NO · 현실 복귀
              </button>
              <button type="button" className="primary" onClick={goToGallery}>
                YES · 판 뒤집기
              </button>
            </div>
          </div>
        )}

        {step === "gallery" && (
          <div className="onboarding-gallery" onPointerDown={handlePointerDown} onPointerUp={handlePointerUp}>
            <div className="onboarding-gallery-counter">
              {galleryIndex + 1}/{ONBOARDING_ENDINGS.length}
            </div>
            <span className="badge onboarding-gallery-badge">{currentEnding.capitalLabel}</span>
            <img
              className="onboarding-gallery-image"
              src={`/capitals/${currentEnding.capitalKey}.png`}
              alt={currentEnding.capitalLabel}
            />
            <h3 className="onboarding-gallery-title">{currentEnding.title}</h3>
            <p className="onboarding-gallery-desc">{currentEnding.description}</p>

            {!isLastCard ? (
              <div className="onboarding-gallery-nav">
                <button type="button" className="ghost" onClick={showPrev} disabled={galleryIndex === 0}>
                  이전
                </button>
                <button type="button" className="primary" onClick={showNext}>
                  다음
                </button>
              </div>
            ) : (
              <div className="onboarding-gallery-final">
                <p className="onboarding-gallery-final-copy">당신의 공주 엔딩을 보여주세요</p>
                <button type="button" className="primary" onClick={() => setDismissed(true)}>
                  프린세스 다이어리 접속하기
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
