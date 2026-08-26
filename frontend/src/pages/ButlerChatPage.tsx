import { useEffect, useState } from "react";
import { getAiFeedbackHistory } from "../api/endpoints";
import type { AiFeedbackHistoryEntry } from "../api/types";

// 레오집사가 지금까지 보내준 코멘트를 전부 이어서 보여주는 채팅 화면 (2026-08-26 요청).
// 예전엔 /record 페이지에 그날 하루치만 일회성으로 떴는데, 그건 RecordPage에 그대로 두고
// (그날 바로 "집사의 한마디 듣기"로 생성하는 용도), 여기서는 지금까지 쌓인 모든 날짜를
// 오래된 순으로 죽 이어붙여서 하나의 채팅 로그처럼 보여준다. 상단 네비바에서 바로 들어온다.
export function ButlerChatPage() {
  const [entries, setEntries] = useState<AiFeedbackHistoryEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAiFeedbackHistory()
      .then(setEntries)
      .catch(() => setError("집사와의 대화를 불러오지 못했어요."));
  }, []);

  return (
    <div className="container">
      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">Butler</span>
        <h1 style={{ fontSize: 26 }}>레오집사</h1>
        <p className="muted">지금까지 레오집사가 남겨준 한마디를 날짜별로 이어서 볼 수 있어요.</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {!error && entries === null && <p className="muted">불러오는 중...</p>}

      {!error && entries !== null && entries.length === 0 && (
        <div className="card">
          <p className="muted">
            아직 레오집사와 나눈 대화가 없어요. "오늘의 기록" 페이지에서 "집사의 한마디 듣기"를 눌러보세요.
          </p>
        </div>
      )}

      {!error && entries !== null && entries.length > 0 && (
        <div className="card">
          <div className="butler-feedback-header">
            <img src="/butler/butler.jpg" alt="" className="butler-avatar" />
            <div className="butler-name-block">
              <span className="butler-name-eyebrow">AI 집사</span>
              <span className="butler-name">레오 집사</span>
            </div>
          </div>

          <div className="stack" style={{ gap: 18 }}>
            {entries.map((entry) => (
              <div key={entry.feedbackDate}>
                <div className="butler-chat-date-divider">
                  <span>{entry.feedbackDate}</span>
                </div>
                <div className="butler-bubbles">
                  {[entry.summary, entry.praise, entry.improvement, entry.tomorrow, entry.cheer]
                    .filter(Boolean)
                    .map((text, i) => (
                      <div className="butler-bubble-row" key={i}>
                        <div className="butler-bubble">{text}</div>
                        <span className="butler-bubble-read">읽음</span>
                      </div>
                    ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
