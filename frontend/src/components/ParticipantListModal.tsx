import { useEffect, useState } from "react";
import { getParticipants } from "../api/endpoints";
import type { ParticipantResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";

// 대시보드 팔로워/팔로잉 클릭 시 뜨는 모달 - 실제 팔로우 관계는 없어서, 같은 기수 다른
// 참가자를 그냥 리스트로 보여준다 (2026-08 요청). 팔로워/팔로잉 어느 쪽을 눌러도 같은 목록이다.
export function ParticipantListModal({ onClose }: { onClose: () => void }) {
  const { user } = useAuth();
  const [participants, setParticipants] = useState<ParticipantResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    getParticipants(user.id)
      .then(setParticipants)
      .catch(() => setError("참가자 목록을 불러오지 못했어요."));
  }, [user]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card participant-modal" onClick={(e) => e.stopPropagation()}>
        <div className="row-between">
          <strong>함께하는 참가자</strong>
          <button type="button" className="ghost participant-modal-close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        {!error && participants === null && <p className="muted">불러오는 중...</p>}

        {!error && participants !== null && participants.length === 0 && (
          <p className="muted">아직 같은 기수로 묶인 다른 참가자가 없어요.</p>
        )}

        {!error && participants !== null && participants.length > 0 && (
          <ul className="participant-list">
            {participants.map((p) => {
              const profileLines = [
                p.goalAppearance && { label: "추구미", value: p.goalAppearance },
                p.goalHuman && { label: "이상향", value: p.goalHuman },
                p.goalEnding && { label: "행동양식", value: p.goalEnding },
              ].filter((line): line is { label: string; value: string } => Boolean(line));
              return (
                <li key={p.id} className="participant-list-item">
                  {p.profileImageUrl ? (
                    <img src={p.profileImageUrl} alt="" className="participant-list-avatar" />
                  ) : (
                    <span className="participant-list-avatar participant-list-avatar-fallback">
                      {p.nickname.slice(0, 1)}
                    </span>
                  )}
                  <div className="participant-list-info">
                    <span className="participant-list-nickname">{p.nickname}</span>
                    {profileLines.map((line) => (
                      <span key={line.label} className="participant-list-goal muted">
                        <strong>{line.label}</strong> · {line.value}
                      </span>
                    ))}
                    {p.instagram && (
                      <span className="participant-list-goal muted">
                        <strong>인스타</strong> ·{" "}
                        <a
                          className="participant-instagram-link"
                          href={`https://www.instagram.com/${encodeURIComponent(p.instagram)}/`}
                          target="_blank"
                          rel="noopener noreferrer"
                          aria-label={`${p.nickname}님의 Instagram 프로필 열기`}
                        >
                          @{p.instagram} ↗
                        </a>
                      </span>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}
