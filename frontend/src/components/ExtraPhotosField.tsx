import { useRef } from "react";

interface ExtraPhotosFieldProps {
  /** 새로 선택했지만 아직 업로드 전인 파일들의 미리보기 URL. */
  previewUrls: string[];
  /** 이미 저장돼 있는(수정 화면에서 불러온) 추가 사진 URL. */
  existingUrls?: string[];
  onAdd: (files: File[]) => void;
  onRemoveNew: (index: number) => void;
  onRemoveExisting?: (index: number) => void;
  maxCount?: number;
}

// 인증사진 여러 장 지원 (2026-09 요청: "인증사진 여러개 들어갈수있었으면 좋겠다"). 대표 사진은
// 기존처럼 카메라 촬영 전용(PhotoCaptureField)이고, 여기서 다루는 "추가 사진"은 참고용 추가
// 증빙이라 갤러리 다중 선택만 지원한다 - AI 판정 대상은 여전히 대표 사진 하나뿐이다.
export function ExtraPhotosField({
  previewUrls,
  existingUrls = [],
  onAdd,
  onRemoveNew,
  onRemoveExisting,
  maxCount = 4,
}: ExtraPhotosFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const totalCount = previewUrls.length + existingUrls.length;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(e.target.files ?? []);
    e.target.value = "";
    if (!selected.length) return;
    const room = Math.max(0, maxCount - totalCount);
    if (room > 0) onAdd(selected.slice(0, room));
  };

  return (
    <div className="stack" style={{ gap: 8 }}>
      <label>추가 사진 (선택, 최대 {maxCount}장)</label>
      {totalCount > 0 && (
        <div className="extra-photo-grid">
          {existingUrls.map((url, i) => (
            <div className="extra-photo-thumb" key={`existing-${i}`}>
              <img src={url} alt={`추가 인증 사진 ${i + 1}`} />
              {onRemoveExisting && (
                <button
                  type="button"
                  className="extra-photo-remove"
                  onClick={() => onRemoveExisting(i)}
                  aria-label="추가 사진 삭제"
                >
                  ×
                </button>
              )}
            </div>
          ))}
          {previewUrls.map((url, i) => (
            <div className="extra-photo-thumb" key={`new-${i}`}>
              <img src={url} alt={`새로 추가한 인증 사진 ${i + 1}`} />
              <button
                type="button"
                className="extra-photo-remove"
                onClick={() => onRemoveNew(i)}
                aria-label="추가 사진 삭제"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}
      {totalCount < maxCount && (
        <button type="button" className="file-picker-button" onClick={() => inputRef.current?.click()}>
          사진 추가하기
        </button>
      )}
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        multiple
        className="visually-hidden-input"
        onChange={handleChange}
      />
    </div>
  );
}
