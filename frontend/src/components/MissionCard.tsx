import { useEffect, useState } from "react";
import { ApiError } from "../api/client";
import { analyzeVisionPhoto, saveRecord, uploadFile } from "../api/endpoints";
import type { GoalTypeCode } from "../api/types";
import { useToast } from "./ToastProvider";

export interface FlatMission {
  userMissionId: number;
  name: string;
  targetValue: number;
  unit: string;
  goalLabel: string;
  goalTypeCode: GoalTypeCode;
}

interface MissionCardProps {
  mission: FlatMission;
  date: string;
  completed: boolean;
  onSaved: () => void;
}

export function MissionCard({ mission, date, completed, onSaved }: MissionCardProps) {
  const { showToast } = useToast();
  const [inputValue, setInputValue] = useState("");
  const [memo, setMemo] = useState("");
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState<string | null>(null);
  const [visionNote, setVisionNote] = useState<{ text: string; ok: boolean } | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Object URLs aren't garbage-collected on their own - revoke the previous one whenever
  // the selected file changes or the card unmounts, so we don't leak blob URLs.
  useEffect(() => {
    return () => {
      if (photoPreviewUrl) URL.revokeObjectURL(photoPreviewUrl);
    };
  }, [photoPreviewUrl]);

  const handlePhotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setPhotoFile(file);
    setVisionNote(null);
    setError(null);
    setPhotoPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return file ? URL.createObjectURL(file) : null;
    });
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
    if (!photoFile) {
      setError("사진을 첨부해야 저장할 수 있어요. 인증 사진을 선택해주세요.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const uploaded = await uploadFile(photoFile);
      await saveRecord({
        userMissionId: mission.userMissionId,
        date,
        inputValue: value,
        photoUrl: uploaded.url,
        memo: memo || undefined,
      });
      onSaved();
      showToast("기록이 저장되었어요");
    } catch (err) {
      if (err instanceof ApiError && err.code === "PHOTO_REQUIRED") {
        setError("사진을 첨부해야 저장할 수 있어요. 인증 사진을 선택해주세요.");
      } else {
        setError("저장에 실패했습니다. 다시 시도해주세요.");
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="habit-tracker-row">
      <div className="row-between">
        <div className="habit-tracker-row-head">
          <div>
            <strong>{mission.name}</strong>
            <div className="muted">
              목표 {mission.targetValue}
              {mission.unit} · {mission.goalLabel}
            </div>
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
        <div className="stack" style={{ gap: 8 }}>
          <label>사진 인증 (필수)</label>
          {photoPreviewUrl && (
            <img src={photoPreviewUrl} alt="첨부한 인증 사진 미리보기" className="photo-preview" />
          )}
          <div className="row" style={{ gap: 10 }}>
            <label htmlFor={`photo-${mission.userMissionId}`} className="file-picker-button">
              {photoFile ? "사진 변경" : "사진 선택"}
            </label>
            <input
              id={`photo-${mission.userMissionId}`}
              type="file"
              accept="image/*"
              onChange={handlePhotoChange}
              className="visually-hidden-input"
            />
            {photoFile && <span className="muted">{photoFile.name}</span>}
          </div>
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
