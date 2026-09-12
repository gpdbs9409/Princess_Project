import { useEffect, useRef, useState } from "react";

interface CameraCaptureModalProps {
  onCapture: (file: File) => void;
  onClose: () => void;
}

/**
 * Opens the device's live camera in-page via getUserMedia and lets the user shoot a frame
 * to a <canvas>, which becomes the verification photo.
 *
 * 2026-08-21 정책 변경: 갤러리 업로드도 다시 허용됐다 - 다만 그건 이 모달이 아니라
 * PhotoCaptureField의 "갤러리에서 선택" 버튼(별도 <input type="file">)을 통해서다. 이 모달
 * 자체는 여전히 실시간 촬영 전용이다. 업로드 단계에서는 촬영 날짜를 검사하지 않는다.
 *
 * 2026-09: 셀카(전면 카메라) 인증 지원 요청 반영 - facingMode를 토글할 수 있게 했다. 기본은
 * 후면("environment")이고, 전면으로 바꾸면 미리보기만 좌우 반전(mirror)해서 자연스럽게 보이게
 * 하되, 실제로 저장되는 사진은 반전하지 않는다 (대부분의 카메라 앱과 동일한 기본 동작).
 */
export function CameraCaptureModal({ onCapture, onClose }: CameraCaptureModalProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [capturedUrl, setCapturedUrl] = useState<string | null>(null);
  const [capturedBlob, setCapturedBlob] = useState<Blob | null>(null);
  const [facingMode, setFacingMode] = useState<"environment" | "user">("environment");
  // 이 기기에 카메라가 두 개 이상 있는지 확실치 않으면 전환 버튼 자체를 숨긴다 - 없는 카메라로
  // 전환을 시도했다가 에러만 보여주는 것보다야 낫다. 목록을 읽을 수 없는 브라우저에서는 그냥
  // 버튼을 보여준다 (getUserMedia의 { ideal } 힌트가 알아서 최선을 골라준다).
  const [canSwitchCamera, setCanSwitchCamera] = useState(true);

  useEffect(() => {
    navigator.mediaDevices?.enumerateDevices?.()
      .then((devices) => {
        const videoInputs = devices.filter((d) => d.kind === "videoinput");
        if (videoInputs.length > 0) setCanSwitchCamera(videoInputs.length > 1);
      })
      .catch(() => {
        // 목록을 못 읽어도 전환 버튼은 그냥 보여준다.
      });
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function start() {
      if (!navigator.mediaDevices?.getUserMedia) {
        setError("이 브라우저에서는 카메라 촬영을 지원하지 않아요. 최신 브라우저로 다시 시도해주세요.");
        return;
      }
      // 전환 시 이전 스트림을 먼저 끄지 않으면 일부 브라우저(특히 모바일)에서 카메라가 계속
      // 점유된 채로 남아 두 번째 getUserMedia 호출이 실패하거나 멈춘다.
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: facingMode } },
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
        setError(null);
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
  }, [facingMode]);

  const handleSwitchCamera = () => {
    setFacingMode((prev) => (prev === "environment" ? "user" : "environment"));
  };

  useEffect(() => {
    return () => {
      if (capturedUrl) URL.revokeObjectURL(capturedUrl);
    };
  }, [capturedUrl]);

  const handleShutter = () => {
    const video = videoRef.current;
    if (!video || !video.videoWidth) return;
    // Phone cameras hand us 1920x1080 or larger, and every one of those pixels gets billed
    // twice: once as OpenAI vision input tokens and once as bucket storage. 1024px on the
    // long edge is still comfortably enough for the model to tell what the photo shows.
    const MAX_EDGE = 1024;
    const scale = Math.min(1, MAX_EDGE / Math.max(video.videoWidth, video.videoHeight));
    const canvas = document.createElement("canvas");
    canvas.width = Math.round(video.videoWidth * scale);
    canvas.height = Math.round(video.videoHeight * scale);
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
      0.85
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
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className="camera-video"
            style={facingMode === "user" ? { transform: "scaleX(-1)" } : undefined}
          />
        )}
        {capturedUrl && <img src={capturedUrl} alt="촬영한 사진 미리보기" className="camera-video" />}

        <p className="muted" style={{ marginTop: 8, fontSize: 13 }}>
          이 화면에서는 지금 바로 촬영한 사진만 쓸 수 있어요. 저장된 사진을 쓰려면 닫고
          "갤러리에서 선택"을 이용해주세요.
        </p>

        <div className="row" style={{ gap: 10, marginTop: 12, justifyContent: "center" }}>
          {!capturedUrl ? (
            <>
              {canSwitchCamera && (
                <button type="button" className="ghost" onClick={handleSwitchCamera} disabled={!!error}>
                  {facingMode === "environment" ? "전면 카메라(셀카)로" : "후면 카메라로"}
                </button>
              )}
              <button type="button" className="primary" onClick={handleShutter} disabled={!!error}>
                촬영하기
              </button>
            </>
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
