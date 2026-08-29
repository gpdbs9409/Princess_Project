import { useRef, useState } from "react";
import { CameraCaptureModal } from "./CameraCapture";

interface PhotoCaptureFieldProps {
  photoFile: File | null;
  photoPreviewUrl: string | null;
  onSelect: (file: File) => void;
  label?: string;
}

// 사진 인증 정책이 다시 바뀌어서 (2026-08-21), 실시간 카메라 촬영에 더해 갤러리(파일) 업로드도
// 같이 허용해야 한다 - 둘 중 아무 방법으로나 사진을 고를 수 있다. 갤러리 선택분은 촬영 날짜와
// 무관하게 허용하며, 사진 내용의 적합성은 별도 AI 판정으로 확인한다.
//
// MissionCard와 공통과제(독서/공부) 양쪽에서 똑같이 쓰기 위해 분리한 컴포넌트. 실제 File 상태는
// 부모가 들고 있고(오브젝트 URL revoke 등 정리 책임도 부모 쪽), 여기는 "카메라로 촬영" vs
// "갤러리에서 선택" 두 입력 경로를 하나의 File로 합쳐서 onSelect로 넘겨주는 역할만 한다.
export function PhotoCaptureField({ photoFile, photoPreviewUrl, onSelect, label }: PhotoCaptureFieldProps) {
  const [showCamera, setShowCamera] = useState(false);
  const galleryInputRef = useRef<HTMLInputElement>(null);

  const handleGalleryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    // 같은 파일을 다시 골라도 onChange가 또 뜨도록 매번 비워준다 (재촬영/재선택 케이스).
    e.target.value = "";
    if (file) onSelect(file);
  };

  const handleCameraCapture = (file: File) => {
    setShowCamera(false);
    onSelect(file);
  };

  return (
    <div className="stack" style={{ gap: 8 }}>
      {label && <label>{label}</label>}
      {photoPreviewUrl && <img src={photoPreviewUrl} alt="선택한 인증 사진 미리보기" className="photo-preview" />}
      <div className="row" style={{ gap: 10, flexWrap: "wrap" }}>
        <button type="button" className="file-picker-button" onClick={() => setShowCamera(true)}>
          {photoFile ? "다시 촬영하기" : "카메라로 촬영하기"}
        </button>
        <button type="button" className="file-picker-button" onClick={() => galleryInputRef.current?.click()}>
          갤러리에서 선택
        </button>
        <input
          ref={galleryInputRef}
          type="file"
          accept="image/*"
          className="visually-hidden-input"
          onChange={handleGalleryChange}
        />
      </div>
      {showCamera && <CameraCaptureModal onCapture={handleCameraCapture} onClose={() => setShowCamera(false)} />}
    </div>
  );
}
