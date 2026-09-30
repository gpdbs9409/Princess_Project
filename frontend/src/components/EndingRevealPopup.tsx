import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getEnding } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import {
  dismissPopupToday,
  fetchEndingStatus,
  hasViewedEnding,
  isEndingPreview,
  popupDismissedToday,
} from "../lib/ending";
import { EndingCardBack } from "./EndingCard";

const SESSION_SHOWN_KEY = "princess_ending_popup_shown";

/**
 * [홈]-P01 엔딩 공개 팝업.
 * 공개 기간(10/1 00:00 ~ 10/20 23:59 KST)에 엔딩을 아직 확인하지 않은 참가자에게 로그인 후
 * 첫 화면 위로 1회 노출한다. [오늘 보지 않기] 이후엔 당일 재노출하지 않고 GNB 탭으로만 재진입.
 * 엔딩 산정이 안 되는 경우(자본 미선택 등)에는 띄우지 않는다.
 */
export function EndingRevealPopup() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!token || !user) return;
    if (location.pathname === "/ending" || location.pathname === "/login") return;
    if (hasViewedEnding(user.id) || popupDismissedToday(user.id)) return;
    try {
      if (sessionStorage.getItem(SESSION_SHOWN_KEY) === "1") return;
    } catch {
      // ignore
    }

    let cancelled = false;
    fetchEndingStatus()
      .then((status) => {
        if (status.phase !== "REVEALED") return null;
        // 산정 가능 여부 확인 (실패하면 미노출)
        return getEnding(isEndingPreview()).then(() => true);
      })
      .then((ready) => {
        if (cancelled || !ready) return;
        try {
          sessionStorage.setItem(SESSION_SHOWN_KEY, "1");
        } catch {
          // ignore
        }
        setOpen(true);
      })
      .catch(() => {
        // 산정 미완료/오류 → 팝업 미노출
      });
    return () => {
      cancelled = true;
    };
    // 로그인 직후엔 /login에 있다가 첫 화면으로 이동하므로 경로가 바뀔 때 다시 판단한다.
    // 한 번 띄우면 세션 플래그로 막히므로 반복 노출되지 않는다.
  }, [token, user, location.pathname]);

  if (!open || !user) return null;

  const close = () => setOpen(false);

  return (
    // 딤 탭으로는 닫히지 않는다 (권장안)
    <div className="modal-overlay ending-popup-overlay" role="dialog" aria-modal="true" aria-labelledby="ending-popup-title">
      <div className="modal-card ending-popup">
        {/* 티저: 카드 뒷면(결과 비노출). 인라인 SVG라 로드 실패가 없다. */}
        <div className="ending-popup-teaser" aria-hidden="true">
          <EndingCardBack width={120} />
        </div>
        <h2 id="ending-popup-title" className="ending-popup-title">
          30일의 여정이 끝났습니다.
          <br />
          당신의 엔딩을 확인하세요
        </h2>
        <button
          type="button"
          className="primary ending-popup-cta"
          onClick={() => {
            close();
            navigate("/ending");
          }}
        >
          엔딩 확인하기
        </button>
        <div className="ending-popup-links">
          <button
            type="button"
            className="link-button"
            onClick={() => {
              dismissPopupToday(user.id);
              close();
            }}
          >
            오늘 보지 않기
          </button>
          <button type="button" className="link-button" onClick={close}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}
