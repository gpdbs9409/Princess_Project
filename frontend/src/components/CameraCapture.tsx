import { useEffect, useRef, useState } from "react";

interface CameraCaptureModalProps {
  onCapture: (file: File) => void;
  onClose: () => void;
}

/**
 * Opens the device's live camera in-page via getUserMedia and lets the user shoot a frame
 * to a <canvas>, which becomes the mission-verification photo. There is intentionally no
 * file picker anywhere in this flow - a canvas-encoded frame never carries EXIF, and the
 * only way to produce one is to point a live camera at something right now, so old gallery
 * photos can't be reused to fake a mission.
 */
export function CameraCaptureModal({ onCapture, onClose }: CameraCaptureModalProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [capturedUrl, setCapturedUrl] = useState<string | null>(null);
  const [capturedBlob, setCapturedBlob] = useState<Blob | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function start() {
      if (!navigator.mediaDevices?.getUserMedia) {
        setError("이 브라우저에서는 카메라 촬영을 지원하지 않아요. 최신 브라우저로 다시 시도해주세요.");
        return;
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: "environment" } },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
      } catch {
        setError("카메라를 사용할 수 없어요. 브라우저 설정에서 카메라 권한을 허용한 뒤 다시 시도해주세요.");
      }
    }

    start();
    return () => {
      cancelled = true;
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    };
  }, []);

  useEffect(() => {
    return () => {
      if (capturedUrl) URL.revokeObjectURL(capturedUrl);
    };
  }, [capturedUrl]);

  const handleShutter = () => {
    const video = videoRef.current;
    if (!video || !video.videoWidth) return;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => {
        if (!blob) return;
        setCapturedBlob(blob);
        setCapturedUrl(URL.createObjectURL(blob));
      },
      "image/jpeg",
      0.92
    );
  };

  const handleRetake = () => {
    setCapturedUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return null;
    });
    setCapturedBlob(null);
  };

  const handleConfirm = () => {
    if (!capturedBlob) return;
    const file = new File([capturedBlob], `mission-${Date.now()}.jpg`, { type: "image/jpeg" });
    streamRef.current?.getTracks().forEach((track) => track.stop());
    onCapture(file);
  };

  return (
    <div className="camera-modal-backdrop" role="dialog" aria-modal="true">
      <div className="camera-modal">
        <div className="row-between" style={{ marginBottom: 10 }}>
          <strong>사진 촬영</strong>
          <button type="button" className="ghost" onClick={onClose}>
            닫기
          </button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        {!error && !capturedUrl && (
          <video ref={videoRef} autoPlay playsInline muted className="camera-video" />
        )}
        {capturedUrl && <img src={capturedUrl} alt="촬영한 사진 미리보기" className="camera-video" />}

        <p className="muted" style={{ marginTop: 8, fontSize: 13 }}>
          갤러리에서 불러온 사진은 사용할 수 없어요. 지금 바로 카메라로 찍어주세요.
        </p>

        <div className="row" style={{ gap: 10, marginTop: 12, justifyContent: "center" }}>
          {!capturedUrl ? (
            <button type="button" className="primary" onClick={handleShutter} disabled={!!error}>
              촬영하기
            </button>
          ) : (
            <>
              <button type="button" className="ghost" onClick={handleRetake}>
                다시 찍기
              </button>
              <button type="button" className="primary" onClick={handleConfirm}>
                이 사진 사용하기
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
