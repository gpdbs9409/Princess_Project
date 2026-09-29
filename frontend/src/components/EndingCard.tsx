import { useState } from "react";
import type { GoalTypeCode } from "../api/types";
import { GOAL_TYPE_LABELS } from "../api/types";
import { CARD_HEIGHT, CARD_WIDTH, ENDING_STAGE_NAMES, endingCardImage, endingCardTitle } from "../lib/ending";

/** 타로 카드 뒷면 실루엣 - 결과는 노출하지 않는다 (공개 팝업 티저 · 공개 전 페이지 · 로딩). */
export function EndingCardBack({ width = CARD_WIDTH, className = "" }: { width?: number; className?: string }) {
  const height = Math.round((width * CARD_HEIGHT) / CARD_WIDTH);
  return (
    <svg
      className={`ending-card-back ${className}`}
      width={width}
      height={height}
      viewBox={`0 0 ${CARD_WIDTH} ${CARD_HEIGHT}`}
      role="img"
      aria-label="엔딩 카드 뒷면"
    >
      <defs>
        <linearGradient id="ecb-bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#3b3350" />
          <stop offset="0.55" stopColor="#5a4668" />
          <stop offset="1" stopColor="#8a5e72" />
        </linearGradient>
        <pattern id="ecb-dots" width="22" height="22" patternUnits="userSpaceOnUse">
          <circle cx="11" cy="11" r="1.2" fill="rgba(255,236,214,0.22)" />
        </pattern>
      </defs>
      <rect x="0" y="0" width={CARD_WIDTH} height={CARD_HEIGHT} rx="16" fill="url(#ecb-bg)" />
      <rect x="0" y="0" width={CARD_WIDTH} height={CARD_HEIGHT} rx="16" fill="url(#ecb-dots)" />
      <rect x="14" y="14" width={CARD_WIDTH - 28} height={CARD_HEIGHT - 28} rx="10" fill="none" stroke="#e8c9a0" strokeWidth="1.5" />
      <rect x="22" y="22" width={CARD_WIDTH - 44} height={CARD_HEIGHT - 44} rx="7" fill="none" stroke="rgba(232,201,160,0.45)" strokeWidth="1" />
      {/* 가운데 문양: 원 + 별 + 왕관 */}
      <g transform={`translate(${CARD_WIDTH / 2} ${CARD_HEIGHT / 2})`} fill="none" stroke="#e8c9a0">
        <circle r="92" strokeWidth="1.2" />
        <circle r="78" strokeWidth="0.8" strokeDasharray="2 5" />
        <path
          d="M0 -62 C4 -18 18 -4 62 0 C18 4 4 18 0 62 C-4 18 -18 4 -62 0 C-18 -4 -4 -18 0 -62 Z"
          fill="rgba(232,201,160,0.16)"
          strokeWidth="1.2"
        />
        <path d="M-22 -2 L-22 -22 L-11 -12 L0 -28 L11 -12 L22 -22 L22 -2 Z" fill="#e8c9a0" stroke="none" />
        <rect x="-22" y="2" width="44" height="5" rx="2" fill="#e8c9a0" stroke="none" />
      </g>
      {[
        [CARD_WIDTH / 2, 70],
        [CARD_WIDTH / 2, CARD_HEIGHT - 70],
      ].map(([x, y]) => (
        <path
          key={y}
          transform={`translate(${x} ${y})`}
          d="M0 -12 C1 -3 3 -1 12 0 C3 1 1 3 0 12 C-1 3 -3 1 -12 0 C-3 -1 -1 -3 0 -12 Z"
          fill="#e8c9a0"
        />
      ))}
    </svg>
  );
}

interface EndingCardFrontProps {
  capital: GoalTypeCode;
  stage: number;
}

/** 엔딩 카드 앞면 - 조합별 고정 시안 이미지. 로드 실패 시 텍스트 카드로 대체. */
export function EndingCardFront({ capital, stage }: EndingCardFrontProps) {
  const [failed, setFailed] = useState(false);
  const title = endingCardTitle(capital, stage);

  if (failed) {
    return (
      <div className="ending-card-fallback">
        <span className="ending-card-tag">
          {GOAL_TYPE_LABELS[capital]} LV.{stage}
        </span>
        <strong className="ending-card-fallback-title">{title}</strong>
        <span className="muted">{ENDING_STAGE_NAMES[stage]}</span>
      </div>
    );
  }

  return (
    <img
      className="ending-card-img"
      src={endingCardImage(capital, stage)}
      alt={`${GOAL_TYPE_LABELS[capital]} LV.${stage} ${title}`}
      width={CARD_WIDTH}
      draggable={false}
      onError={() => setFailed(true)}
    />
  );
}
