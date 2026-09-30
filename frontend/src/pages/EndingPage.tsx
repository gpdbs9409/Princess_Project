import { useCallback, useEffect, useRef, useState } from "react";
import { getActiveProject, getEnding } from "../api/endpoints";
import type { EndingResponse, EndingStatusResponse, ProjectResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { EndingCardBack, EndingCardFront } from "../components/EndingCard";
import { EndingStatChart } from "../components/EndingStatChart";
import { SideWidget } from "../components/SideWidget";
import { useToast } from "../components/ToastProvider";
import {
  CARD_HEIGHT,
  CARD_WIDTH,
  OPEN_CHAT_URL,
  endingCardImage,
  isEndingPreview,
  isInAppBrowser,
  markEndingVisited,
  useEndingStatus,
} from "../lib/ending";
import { downloadBlob, renderCardPng } from "../lib/endingImage";

const SLOW_LOADING_MS = 2000;
const MAX_FAILURES_BEFORE_CONTACT = 2;

function pad(n: number) {
  return String(n).padStart(2, "0");
}

// ---------------------------------------------------------------------------
// UI-0X-01 엔딩 공개 전 페이지
// ---------------------------------------------------------------------------

function BeforeReveal({ status }: { status: EndingStatusResponse }) {
  // 서버 시각 기준으로 카운트다운 (기기 시계가 틀려도 공개 시각은 서버와 맞춘다)
  const [offset] = useState(() => new Date(status.serverNow).getTime() - Date.now());
  const [now, setNow] = useState(() => Date.now() + offset);
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now() + offset), 1000);
    return () => clearInterval(id);
  }, [offset]);

  const revealAt = new Date(status.revealAt).getTime();
  const remaining = revealAt - now;
  const revealed = remaining <= 0;
  const totalMinutes = Math.max(0, Math.ceil(remaining / 60000));
  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;

  return (
    <div className="ending-before">
      <div className="ending-card-frame">
        <EndingCardBack />
      </div>
      <p className="ending-before-text">공주 엔딩은 10/1에 공개됩니다.</p>
      {revealed ? (
        <div className="stack" style={{ alignItems: "center", gap: 10 }}>
          <span className="ending-dday-chip">엔딩이 공개되었어요</span>
          <button type="button" className="primary" onClick={() => window.location.reload()}>
            새로고침해서 확인하기
          </button>
        </div>
      ) : (
        <span className="ending-dday-chip tabular" aria-live="polite">
          {days > 0 ? `D-${days}` : "D-DAY"} {pad(hours)}:{pad(minutes)}
        </span>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// UI-0X-02 엔딩 페이지
// ---------------------------------------------------------------------------

function Revealed({ nickname }: { nickname: string }) {
  const { showToast } = useToast();
  const [ending, setEnding] = useState<EndingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [slow, setSlow] = useState(false);
  const [error, setError] = useState<"load" | "unavailable" | null>(null);
  const [failures, setFailures] = useState(0);
  const [showContact, setShowContact] = useState(false);
  const [flipped, setFlipped] = useState(false);
  const [busy, setBusy] = useState<"save" | null>(null);
  const [longPressImage, setLongPressImage] = useState<string | null>(null);
  const failuresRef = useRef(0);

  const load = useCallback(() => {
    setLoading(true);
    setSlow(false);
    setError(null);
    setFlipped(false);
    const slowTimer = setTimeout(() => setSlow(true), SLOW_LOADING_MS);
    getEnding(isEndingPreview())
      .then((data) => {
        setEnding(data);
        failuresRef.current = 0;
        setFailures(0);
        // 카드 뒤집기 연출 (결과 공개 연출 겸용)
        requestAnimationFrame(() => setTimeout(() => setFlipped(true), 60));
      })
      .catch((err: { code?: string }) => {
        if (err?.code === "ENDING_NOT_AVAILABLE") {
          setError("unavailable");
          return;
        }
        failuresRef.current += 1;
        setFailures(failuresRef.current);
        setError("load");
        if (failuresRef.current > MAX_FAILURES_BEFORE_CONTACT) setShowContact(true);
      })
      .finally(() => {
        clearTimeout(slowTimer);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const cardSrc = ending ? endingCardImage(ending.capital, ending.stage) : "";

  const saveCard = async () => {
    if (!ending || busy) return;
    setBusy("save");
    try {
      const blob = await renderCardPng(cardSrc);
      if (isInAppBrowser()) {
        setLongPressImage(URL.createObjectURL(blob));
      } else {
        downloadBlob(blob, `princess-ending-${ending.capital.toLowerCase()}-${ending.stage}.png`);
        showToast("이미지가 저장되었어요");
      }
    } catch {
      showToast("이미지를 만들지 못했어요. 다시 시도해주세요");
    } finally {
      setBusy(null);
    }
  };

  return (
    <>
      <h1 className="ending-title">{nickname}님의 엔딩</h1>

      <div className="ending-card-frame" style={{ width: CARD_WIDTH, height: CARD_HEIGHT }}>
        {error ? (
          <div className="ending-card-error">
            {error === "unavailable" ? (
              <p>엔딩을 산정할 기록이 없어요.</p>
            ) : (
              <>
                <p>엔딩을 불러오지 못했어요</p>
                <button type="button" className="primary" onClick={load}>
                  다시 시도
                </button>
                {failures > MAX_FAILURES_BEFORE_CONTACT && (
                  <a className="muted" href={OPEN_CHAT_URL} target="_blank" rel="noreferrer">
                    1:1 오픈채팅으로 문의하기
                  </a>
                )}
              </>
            )}
          </div>
        ) : (
          <div className={`ending-flip ${flipped ? "is-flipped" : ""} ${loading ? "is-loading" : ""}`}>
            <div className="ending-flip-face ending-flip-back">
              <EndingCardBack />
              {loading && slow && <span className="ending-loading-label">엔딩을 불러오는 중...</span>}
            </div>
            <div className="ending-flip-face ending-flip-front">
              {ending && <EndingCardFront capital={ending.capital} stage={ending.stage} />}
            </div>
          </div>
        )}
      </div>

      {ending && (ending.mvpApplied || ending.mvpTeaTime) && (
        <p className="ending-mvp-note">
          {ending.mvpApplied
            ? "✦ 주간 MVP 성장권으로 결말이 한 단계 올라갔어요"
            : "✦ 이미 공주 엔딩이라, MVP 성장권은 티타임으로 안내드릴게요"}
        </p>
      )}

      <div className="ending-actions">
        <button type="button" className="primary ending-action" disabled={!ending || busy !== null} onClick={saveCard}>
          {busy === "save" ? "저장 중..." : "이미지 저장"}
        </button>
      </div>

      <div className="section" style={{ marginTop: 32 }}>
        <div className="section-band">나의 한달동안의 기록</div>
        <div className="card">
          {ending ? (
            <EndingStatChart seriesKeys={ending.seriesKeys} weeks={ending.weeks} />
          ) : (
            <p className="muted ending-chart-empty">{loading ? "불러오는 중..." : "기록된 스탯이 없습니다"}</p>
          )}
          {ending && ending.weeks.length > 0 && (
            <p className="muted ending-chart-caption">
              {ending.weeks[0].start.slice(5).replace("-", "/")} ~ {ending.weeks[ending.weeks.length - 1].end.slice(5).replace("-", "/")} 누적 점수
            </p>
          )}
        </div>
      </div>

      {longPressImage && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-card ending-longpress">
            <p style={{ margin: 0, fontWeight: 600 }}>이미지를 길게 눌러 저장하세요</p>
            <img src={longPressImage} alt="저장할 엔딩 이미지" />
            <button
              type="button"
              className="primary"
              onClick={() => {
                URL.revokeObjectURL(longPressImage);
                setLongPressImage(null);
              }}
            >
              확인
            </button>
          </div>
        </div>
      )}

      {showContact && (
        <div className="modal-overlay" role="alertdialog" aria-modal="true">
          <div className="modal-card">
            <p style={{ margin: 0 }}>
              엔딩을 계속 불러오지 못하고 있어요.
              <br />
              1:1 오픈채팅으로 문의해주세요.
            </p>
            <a className="button-link primary" href={OPEN_CHAT_URL} target="_blank" rel="noreferrer">
              1:1 오픈채팅 열기
            </a>
            <button type="button" className="ghost" onClick={() => setShowContact(false)}>
              닫기
            </button>
          </div>
        </div>
      )}
    </>
  );
}

// ---------------------------------------------------------------------------

export function EndingPage() {
  const { user } = useAuth();
  const { status, error, reload } = useEndingStatus(true);
  const [project, setProject] = useState<ProjectResponse | null>(null);

  useEffect(() => {
    getActiveProject().then(setProject).catch(() => {});
  }, []);

  useEffect(() => {
    // 공개 기간 첫 진입 시 N 뱃지 해제 + 공개 팝업 재노출 중단
    if (status?.phase === "REVEALED" && user) markEndingVisited(user.id);
  }, [status?.phase, user]);

  if (!user) return null;

  return (
    <div className="container ending-page">
      <SideWidget project={project} />
      {status?.preview && <div className="ending-preview-banner">관리자 미리보기 모드 (?endingPreview=0 으로 해제)</div>}
      {error && (
        <div className="ending-card-error" style={{ margin: "40px auto" }}>
          <p>엔딩을 불러오지 못했어요</p>
          <button type="button" className="primary" onClick={reload}>
            다시 시도
          </button>
        </div>
      )}
      {!status && !error && (
        <div className="ending-card-frame" style={{ marginTop: 48 }}>
          <EndingCardBack />
        </div>
      )}
      {status?.phase === "BEFORE_REVEAL" && <BeforeReveal status={status} />}
      {status?.phase === "REVEALED" && <Revealed nickname={user.nickname} />}
      {status?.phase === "CLOSED" && (
        <p className="ending-before-text" style={{ marginTop: 48 }}>
          프린세스 프로젝트 1기 운영이 종료되었어요. 함께해 주셔서 감사합니다.
        </p>
      )}
    </div>
  );
}
