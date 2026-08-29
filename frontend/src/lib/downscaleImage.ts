/**
 * 업로드 전에 이미지를 줄인다.
 *
 * 카메라 촬영은 CameraCapture에서 이미 캔버스로 축소해서 넘기는데, 갤러리 선택은 원본
 * 파일이 그대로 올라간다. 요즘 폰 사진이 3~10MB라 그대로 두면
 *   - Vision 호출 시 백엔드가 Base64로 인코딩하면서 요청 1건당 힙을 수십 MB 잡고
 *   - 인증 사진이 42KB가 아니라 수 MB씩 버킷에 쌓이며
 *   - 캡처 해상도를 1,024px로 낮춰서 얻은 AI 비용 절감이 그대로 새어나간다.
 * 그래서 두 경로 모두 같은 규격으로 맞춘다.
 */
export const MAX_IMAGE_EDGE = 1024;
export const IMAGE_JPEG_QUALITY = 0.85;

/**
 * 실패해도 절대 던지지 않는다. 디코딩할 수 없는 포맷(브라우저가 지원하지 않는 HEIC 등)이면
 * 원본을 그대로 돌려준다 - 용량을 줄이자고 인증 자체를 막을 수는 없기 때문이다.
 */
export async function downscaleImageFile(file: File): Promise<File> {
  if (!file.type.startsWith("image/")) return file;

  try {
    const bitmap = await loadBitmap(file);
    const scale = Math.min(1, MAX_IMAGE_EDGE / Math.max(bitmap.width, bitmap.height));

    // 이미 충분히 작으면 재인코딩으로 화질만 깎을 이유가 없다.
    if (scale === 1 && file.size <= 400_000) {
      bitmap.close?.();
      return file;
    }

    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));

    const ctx = canvas.getContext("2d");
    if (!ctx) return file;
    ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    bitmap.close?.();

    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, "image/jpeg", IMAGE_JPEG_QUALITY)
    );
    if (!blob) return file;

    // 줄였는데 오히려 커졌으면(이미 잘 압축된 작은 파일) 원본을 쓴다.
    if (blob.size >= file.size) return file;

    return new File([blob], toJpegName(file.name), {
      type: "image/jpeg",
      lastModified: Date.now(),
    });
  } catch {
    return file;
  }
}

/**
 * createImageBitmap이 있으면 그걸 쓴다 - 메인 스레드 부담이 적고, imageOrientation으로
 * EXIF 회전이 반영돼서 세로로 찍은 사진이 눕지 않는다. 없으면 <img>로 폴백한다.
 */
async function loadBitmap(file: File): Promise<ImageBitmap> {
  if (typeof createImageBitmap === "function") {
    try {
      return await createImageBitmap(file, { imageOrientation: "from-image" });
    } catch {
      // 일부 브라우저는 옵션 인자를 거부한다 - 옵션 없이 한 번 더 시도
      return await createImageBitmap(file);
    }
  }

  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error("image decode failed"));
      el.src = url;
    });
    return (await createImageBitmap(img)) as ImageBitmap;
  } finally {
    URL.revokeObjectURL(url);
  }
}

function toJpegName(name: string): string {
  const base = name.replace(/\.[^.]+$/, "") || "photo";
  return `${base}.jpg`;
}
