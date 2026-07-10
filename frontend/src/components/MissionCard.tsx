import { useState } from "react";
import { analyzeVisionPhoto, saveRecord, uploadFile } from "../api/endpoints";

export interface FlatMission {
  userMissionId: number;
  name: string;
  targetValue: number;
  unit: string;
  goalLabel: string;
}

interface MissionCardProps {
  mission: FlatMission;
  date: string;
  completed: boolean;
  onSaved: () => void;
}

export function MissionCard({ mission, date, completed, onSaved }: MissionCardProps) {
  const [inputValue, setInputValue] = useState("");
  const [memo, setMemo] = useState("");
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [visionNote, setVisionNote] = useState<{ text: string; ok: boolean } | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handlePhotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setPhotoFile(file);
    setVisionNote(null);
    if (!file) return;
    try {
      const result = await analyzeVisionPhoto(file, mission.name);
      setVisionNote({ text: result.reason, ok: result.likelyValid });
    } catch {
      setVisionNote({ text: "사진 확인 중 오류가 발생했습니다.", ok: false });
    }
  };

  const handleSave = async () => {
    const value = Number(inputValue);
    if (!inputValue || Number.isNaN(value)) {
      setError("입력값을 확인해주세요.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      let photoUrl: string | undefined;
      if (photoFile) {
        const uploaded = await uploadFile(photoFile);
        photoUrl = uploaded.url;
      }
      await saveRecord({
        userMissionId: mission.userMissionId,
        date,
        inputValue: value,
        photoUrl,
        memo: memo || undefined,
      });
      onSaved();
    } catch {
      setError("저장에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="card">
      <div className="row-between" style={{ marginBottom: 10 }}>
        <div>
          <strong>{mission.name}</strong>
          <div className="muted">
            목표 {mission.targetValue}
            {mission.unit} · {mission.goalLabel}
          </div>
        </div>
        {completed && <span className="badge good">완료</span>}
      </div>
      <div className="stack" style={{ gap: 10 }}>
        <div className="row" style={{ gap: 8 }}>
          <input
            type="number"
            placeholder={`${mission.unit} 단위로 입력`}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            style={{ maxWidth: 140 }}
          />
          <span className="muted">{mission.unit}</span>
        </div>
        <input
          type="text"
          placeholder="오늘 어땠나요? (선택)"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
        />
        <div className="stack" style={{ gap: 4 }}>
          <label>사진 인증 (선택)</label>
          <input type="file" accept="image/*" onChange={handlePhotoChange} />
          {visionNote && (
            <span className="muted" style={{ color: visionNote.ok ? "var(--good)" : "var(--warn)" }}>
              {visionNote.text}
            </span>
          )}
        </div>
        {error && <div className="error-banner">{error}</div>}
        <button className="primary" onClick={handleSave} disabled={saving}>
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
    </div>
  );
}
