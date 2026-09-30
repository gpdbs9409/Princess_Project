// 엔딩 카드 이미지 저장 (화면설계서 7p 4번).
// 인스타그램 스토리 공유(7p 5번, 9p)는 2026-09-30 스펙아웃.
// 카드 시안 PNG를 그대로 캔버스에 그리므로 그래프·점수는 절대 포함되지 않는다.

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(`image load failed: ${src}`));
    img.src = src;
  });
}

function canvasToBlob(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error("toBlob failed"))), "image/png");
  });
}

/** 카드만 PNG · 2배 해상도(폭 660px). */
export async function renderCardPng(cardSrc: string): Promise<Blob> {
  const img = await loadImage(cardSrc);
  const scale = 660 / img.naturalWidth;
  const canvas = document.createElement("canvas");
  canvas.width = 660;
  canvas.height = Math.round(img.naturalHeight * scale);
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("no 2d context");
  ctx.imageSmoothingQuality = "high";
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
  return canvasToBlob(canvas);
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
