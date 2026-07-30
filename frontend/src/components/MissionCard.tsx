import { useEffect, useState } from "react";
import { ApiError } from "../api/client";
import { analyzeVisionPhoto, saveRecord, uploadFile } from "../api/endpoints";
import type { GoalTypeCode, TodayRecordEntry } from "../api/types";
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
  record?: TodayRecordEntry;
  onSaved: () => void;
}

export function MissionCard({ mission, date, completed, record, onSaved }: MissionCardProps) {
  const { showToast } = useToast();
  const [inputValue, setInputValue] = useState("");
  const [memo, setMemo] = useState("");
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState<string | null>(null);
  const [visionNote, setVisionNote] = useState<{ text: string; ok: boolean } | null>(null);
  const [checkingVision, setCheckingVision] = useState(false);
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
    setCheckingVision(true);
    try {
      const result = await analyzeVisionPhoto(file, mission.name);
      setVisionNote({ text: result.reason, ok: result.likelyValid });
    } catch {
      // Unknown either way (call failed, not "photo looks wrong") - don't silently record this
      // as a false verification result.
      setVisionNote({ text: "사진 확인 중 오류가 발생했습니다.", ok: false });
    } finally {
      setCheckingVision(false);
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
    if (checkingVision) {
      setError("사진 확인이 끝날 때까지 잠시만 기다려주세요.");
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
        aiVerified: visionNote?.ok,
      });
      onSaved();
      showToast("기록이 저장되었어요");
    } catch (err) {
      if (err instanceof ApiError && err.code === "PHOTO_REQUIRED") {
        setError("사진을 첨부해야 저장할 수 있어요. 인증 사진을 선택해주세요.");
      } else if (err instanceof ApiError && err.code === "PHOTO_NOT_FROM_TODAY") {
        setError("오늘 찍은 사진이 아니에요. 오늘 찍은 사진으로 바꿔주세요.");
      } else if (err instanceof ApiError && err.code === "PHOTO_DATE_UNKNOWN") {
        setError("사진 촬영 날짜를 확인할 수 없어요. 오늘 찍은 사진으로 바꿔주세요.");
      } else {
        // Not one of our known validation codes - log the real cause so it's debuggable
        // instead of only ever showing the generic message below.
        console.error("saveRecord failed", err);
        setError("저장에 실패했습니다. 다시 시도해주세요.");
      }
    } finally {
      setSaving(false);
    }
  };

  if (record) {
    return (
      <div className="card recorded-mission-card">
        <div className="row-between">
          <div>
            <strong>{mission.name}</strong>
            <div className="muted">
              목표 {mission.targetValue}
              {mission.unit} · {mission.goalLabel}
            </div>
          </div>
          {completed && <span className="badge good">완료</span>}
        </div>
        <div className="stack" style={{ gap: 10, marginTop: 12 }}>
          <div className="recorded-field">
            <span className="muted">기록한 값</span>
            <strong>
              {record.inputValue}
              {mission.unit}
            </strong>
          </div>
          <div className="recorded-field">
            <span className="muted">오늘의 소감</span>
            <span>{record.memo || "작성하지 않았어요"}</span>
          </div>
          {record.photoUrl && (
            <img src={record.photoUrl} alt="기록한 인증 사진" className="photo-preview" />
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="row-between">
        <div>
          <strong>{mission.name}</strong>
          <div className="muted">
            목표 {mission.targetValue}
            {mission.unit} · {mission.goalLabel}
          </div>
        </div>
        {completed && <span className="badge good">완료</span>}
      </div>
      <div className="stack" style={{ gap: 10, marginTop: 12 }}>
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
          {checkingVision && <span className="muted">사진 확인 중...</span>}
          {!checkingVision && visionNote && (
            <span className="muted" style={{ color: visionNote.ok ? "var(--good)" : "var(--warn)" }}>
              {visionNote.text}
            </span>
          )}
        </div>
        {error && <div className="error-banner">{error}</div>}
        <button className="primary" onClick={handleSave} disabled={saving || checkingVision}>
          {saving ? "저장 중..." : checkingVision ? "사진 확인 중..." : "저장"}
        </button>
      </div>
    </div>
  );
}
