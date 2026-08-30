import { useEffect, useState } from "react";
import { generateAiFeedback, getAiFeedbackHistory } from "../api/endpoints";
import type { AiFeedbackHistoryEntry } from "../api/types";

// 레오집사가 지금까지 보내준 코멘트를 전부 이어서 보여주는 채팅 화면 (2026-08-26 요청).
// 피드백 생성과 누적 대화를 모두 이 전용 화면에서 처리한다. 오늘 기록 화면에는 중복된 레오집사
// 섹션을 두지 않는다.
export function ButlerChatPage() {
  const [entries, setEntries] = useState<AiFeedbackHistoryEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);

  const todayIso = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  const loadHistory = () =>
    getAiFeedbackHistory()
      .then(setEntries)
      .catch(() => setError("집사와의 대화를 불러오지 못했어요."));

  useEffect(() => {
    loadHistory();
  }, []);

  const handleGenerateFeedback = async () => {
    setGenerating(true);
    setError(null);
    try {
      await generateAiFeedback(todayIso());
      await loadHistory();
    } catch {
      setError("집사의 한마디를 받아오지 못했어요. 오늘 기록을 먼저 저장한 뒤 다시 시도해주세요.");
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="container">
      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">Butler</span>
        <h1 style={{ fontSize: 26 }}>레오집사</h1>
        <p className="muted">지금까지 레오집사가 남겨준 한마디를 날짜별로 이어서 볼 수 있어요.</p>
        <button
          type="button"
          className="primary"
          onClick={handleGenerateFeedback}
          disabled={generating}
          style={{ alignSelf: "flex-start" }}
        >
          {generating ? "집사가 오늘 하루를 살펴보는 중..." : "오늘의 한마디 듣기"}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {!error && entries === null && <p className="muted">불러오는 중...</p>}

      {!error && entries !== null && entries.length === 0 && (
        <div className="card">
          <p className="muted">아직 레오집사와 나눈 대화가 없어요. 위 버튼을 눌러 오늘의 한마디를 받아보세요.</p>
        </div>
      )}

      {!error && entries !== null && entries.length > 0 && (
        <div className="card butler-chat-scene">
          <div className="butler-feedback-header">
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
