import { useEffect, useState } from "react";
import { ApiError } from "../api/client";
import { analyzeVisionPhoto, saveRecord, uploadFile } from "../api/endpoints";
import type { GoalTypeCode, TodayRecordEntry } from "../api/types";
import { ExtraPhotosField } from "./ExtraPhotosField";
import { PhotoCaptureField } from "./PhotoCaptureField";
import { useToast } from "./ToastProvider";

// 갤러리에서 한 번에 고를 수 있는 추가 사진 개수 상한 - ExtraPhotosField의 기본값과 맞춘다.
const MAX_EXTRA_PHOTOS = 4;

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
  readOnly?: boolean;
}

// Mirrors the backend's DailyRecordService.MAX_INPUT_MULTIPLE_OF_TARGET - generous headroom
// above the mission's own target so genuine overachievement (walking 15,000 steps against a
// 10,000 target) always fits, while still catching fat-finger/garbage input like "999999"
// typed into a "30분" mission.
const MAX_INPUT_MULTIPLE_OF_TARGET = 50;

export function MissionCard({ mission, date, completed, record, onSaved, readOnly = false }: MissionCardProps) {
  const { showToast } = useToast();
  const [inputValue, setInputValue] = useState("");
  const [memo, setMemo] = useState("");
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState<string | null>(null);
  const [visionNote, setVisionNote] = useState<{ text: string; ok: boolean } | null>(null);
  const [checkingVision, setCheckingVision] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  // 참고용 추가 사진(2026-09) - 대표 사진과 달리 AI 판정 대상은 아니고, 갤러리에서 여러 장
  // 골라 첨부만 하는 보조 증빙이다. existingExtraUrls는 수정 화면에서 불러온 기존 저장분,
  // extraPhotoFiles/extraPhotoPreviewUrls는 이번에 새로 고른 것.
  const [existingExtraUrls, setExistingExtraUrls] = useState<string[]>([]);
  const [extraPhotoFiles, setExtraPhotoFiles] = useState<File[]>([]);
  const [extraPhotoPreviewUrls, setExtraPhotoPreviewUrls] = useState<string[]>([]);

  useEffect(() => {
    if (!record || !editing) return;
    setInputValue(String(record.inputValue));
    setMemo(record.memo ?? "");
    setPhotoFile(null);
    setPhotoPreviewUrl(record.photoUrl);
    setVisionNote(null);
    setError(null);
    setExistingExtraUrls(record.extraPhotoUrls ?? []);
    setExtraPhotoFiles([]);
    setExtraPhotoPreviewUrls([]);
  }, [record, editing]);

  // Object URLs aren't garbage-collected on their own - revoke the previous one whenever
  // the selected file changes or the card unmounts, so we don't leak blob URLs.
  useEffect(() => {
    return () => {
      if (photoPreviewUrl) URL.revokeObjectURL(photoPreviewUrl);
    };
  }, [photoPreviewUrl]);

  useEffect(() => {
    return () => {
      extraPhotoPreviewUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [extraPhotoPreviewUrls]);

  const handleAddExtraPhotos = (files: File[]) => {
    setExtraPhotoFiles((prev) => [...prev, ...files]);
    setExtraPhotoPreviewUrls((prev) => [...prev, ...files.map((f) => URL.createObjectURL(f))]);
  };

  const handleRemoveNewExtraPhoto = (index: number) => {
    setExtraPhotoPreviewUrls((prev) => {
      const url = prev[index];
      if (url) URL.revokeObjectURL(url);
      return prev.filter((_, i) => i !== index);
    });
    setExtraPhotoFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleRemoveExistingExtraPhoto = (index: number) => {
    setExistingExtraUrls((prev) => prev.filter((_, i) => i !== index));
  };

  // 2026-09: 사진만으로 부적합(false) 판정이 났을 때, 사용자가 메모에 설명을 적고 나가면
  // (블러) 메모까지 포함해서 한 번 더 판정한다 (백엔드 2차 판정 로직과 짝). 매 타이핑마다
  // 부르면 낭비니 블러 시점에만 실행하고, 이미 true거나 메모가 비어있으면 건드리지 않는다.
  const handleMemoBlur = async () => {
    if (!photoFile || !visionNote || visionNote.ok || !memo.trim() || checkingVision) return;
    setCheckingVision(true);
    try {
      const result = await analyzeVisionPhoto(photoFile, mission.name, memo);
      setVisionNote({ text: result.reason, ok: result.likelyValid });
    } catch {
      // 재판정 실패 시 기존 판정을 그대로 둔다 - 저장 자체를 막지는 않는다.
    } finally {
      setCheckingVision(false);
    }
  };

  // There is no gallery/file picker for mission photos - only the live in-app camera. A
  // canvas-captured frame can't be a re-uploaded old photo, so this is the anti-cheat
  // measure itself, not just a UX choice (see CameraCapture.tsx).
  const handlePhotoCaptured = async (file: File) => {
    setPhotoFile(file);
    setVisionNote(null);
    setError(null);
    setPhotoPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return URL.createObjectURL(file);
    });
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
    if (value <= 0) {
      setError("0보다 큰 값을 입력해주세요.");
      return;
    }
    const maxReasonableValue = mission.targetValue * MAX_INPUT_MULTIPLE_OF_TARGET;
    if (value > maxReasonableValue) {
      setError(
        `입력값이 너무 커요. 목표(${mission.targetValue}${mission.unit})의 ${MAX_INPUT_MULTIPLE_OF_TARGET}배 이하로 입력해주세요.`
      );
      return;
    }
    if (!photoFile && !record?.photoUrl) {
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
      const uploaded = photoFile ? await uploadFile(photoFile) : null;
      const uploadedExtras = extraPhotoFiles.length
        ? await Promise.all(extraPhotoFiles.map((file) => uploadFile(file)))
        : [];
      await saveRecord({
        userMissionId: mission.userMissionId,
        date,
        inputValue: value,
        photoUrl: uploaded?.url ?? record?.photoUrl ?? undefined,
        memo: memo || undefined,
        aiVerified: photoFile ? visionNote?.ok : record?.aiVerified ?? undefined,
        extraPhotoUrls: [...existingExtraUrls, ...uploadedExtras.map((u) => u.url)],
      });
      onSaved();
      setEditing(false);
      showToast(record ? "기록이 수정되었어요" : "기록이 저장되었어요");
    } catch (err) {
      if (err instanceof ApiError && err.code === "PHOTO_REQUIRED") {
        setError("사진을 첨부해야 저장할 수 있어요. 인증 사진을 선택해주세요.");
      } else if (err instanceof ApiError && err.code === "INPUT_NEGATIVE") {
        setError("입력값은 0보다 커야 해요.");
      } else if (err instanceof ApiError && err.code === "INPUT_TOO_LARGE") {
        setError(`입력값이 너무 커요. 목표(${mission.targetValue}${mission.unit})에 비해 지나치게 큰 값이에요.`);
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

  if (record && !editing) {
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
            <span className="muted">기록한 소감</span>
            <span>{record.memo || "작성하지 않았어요"}</span>
          </div>
          {record.photoUrl && (
            <img src={record.photoUrl} alt="기록한 인증 사진" className="photo-preview" />
          )}
          {record.extraPhotoUrls && record.extraPhotoUrls.length > 0 && (
            <div className="extra-photo-grid">
              {record.extraPhotoUrls.map((url, i) => (
                <div className="extra-photo-thumb" key={i}>
                  <img src={url} alt={`추가 인증 사진 ${i + 1}`} />
                </div>
              ))}
            </div>
          )}
          {!readOnly && (
            <button type="button" className="ghost" onClick={() => setEditing(true)}>
              기록 수정
            </button>
          )}
        </div>
      </div>
    );
  }

  if (readOnly) return null;

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
            min={0}
            max={mission.targetValue * MAX_INPUT_MULTIPLE_OF_TARGET}
            placeholder={`${mission.unit} 단위로 입력`}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            style={{ maxWidth: 140 }}
          />
          <span className="muted">{mission.unit}</span>
        </div>
        <input
          type="text"
          placeholder="오늘 어땠나요? (선택 · 사진 판정이 '부적합'이면 여기 설명을 남겨보세요)"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          onBlur={handleMemoBlur}
        />
        <div className="stack" style={{ gap: 8 }}>
          <PhotoCaptureField
            photoFile={photoFile}
            photoPreviewUrl={photoPreviewUrl}
            onSelect={handlePhotoCaptured}
            label="사진 인증 (필수 · 카메라 촬영 또는 갤러리에서 선택)"
          />
          {checkingVision && <span className="muted">사진 확인 중...</span>}
          {!checkingVision && visionNote && (
            <span className="muted" style={{ color: visionNote.ok ? "var(--good)" : "var(--warn)" }}>
              {visionNote.text}
            </span>
          )}
          <ExtraPhotosField
            previewUrls={extraPhotoPreviewUrls}
            existingUrls={existingExtraUrls}
            onAdd={handleAddExtraPhotos}
            onRemoveNew={handleRemoveNewExtraPhoto}
            onRemoveExisting={handleRemoveExistingExtraPhoto}
            maxCount={MAX_EXTRA_PHOTOS}
          />
        </div>
        {error && <div className="error-banner">{error}</div>}
        <div className="row" style={{ gap: 8 }}>
          <button className="primary" onClick={handleSave} disabled={saving || checkingVision}>
            {saving ? "저장 중..." : checkingVision ? "사진 확인 중..." : editing ? "수정 저장" : "저장"}
          </button>
          {editing && <button type="button" className="ghost" onClick={() => setEditing(false)} disabled={saving}>취소</button>}
        </div>
      </div>
    </div>
  );
}
