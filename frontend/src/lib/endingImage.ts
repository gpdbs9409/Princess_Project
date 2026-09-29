// 엔딩 카드 이미지 저장 / 인스타그램 스토리 이미지 생성 (화면설계서 7p 4·5번, 9p).
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

function roundRectPath(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
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

/** 인스타그램 스토리용 9:16 (1080×1920): 배경 + 카드 가운데 + 서비스명. */
export async function renderStoryPng(cardSrc: string): Promise<Blob> {
  const img = await loadImage(cardSrc);
  try {
    await document.fonts?.load('700 44px "Pretendard Variable"');
  } catch {
    // 폰트 로드 실패 시 시스템 폰트로 그린다
  }

  const W = 1080;
  const H = 1920;
  const canvas = document.createElement("canvas");
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("no 2d context");

  const bg = ctx.createLinearGradient(0, 0, 0, H);
  bg.addColorStop(0, "#faf7f3");
  bg.addColorStop(0.55, "#f3e4e4");
  bg.addColorStop(1, "#e9cfd3");
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, W, H);

  // 은은한 별 장식
  ctx.fillStyle = "rgba(212, 138, 148, 0.35)";
  const stars: Array<[number, number, number]> = [
    [140, 260, 10], [930, 330, 14], [120, 1580, 12], [960, 1650, 9], [540, 150, 7], [880, 1500, 6], [220, 1760, 6],
  ];
  for (const [x, y, r] of stars) {
    ctx.beginPath();
    ctx.moveTo(x, y - r * 2);
    ctx.quadraticCurveTo(x, y, x + r * 2, y);
    ctx.quadraticCurveTo(x, y, x, y + r * 2);
    ctx.quadraticCurveTo(x, y, x - r * 2, y);
    ctx.quadraticCurveTo(x, y, x, y - r * 2);
    ctx.fill();
  }

  const cardW = 760;
  const cardH = Math.round((img.naturalHeight / img.naturalWidth) * cardW);
  const cardX = (W - cardW) / 2;
  const cardY = Math.round((H - cardH) / 2) - 40;

  ctx.save();
  ctx.shadowColor = "rgba(60, 30, 40, 0.22)";
  ctx.shadowBlur = 50;
  ctx.shadowOffsetY = 18;
  roundRectPath(ctx, cardX, cardY, cardW, cardH, 28);
  ctx.fillStyle = "#faf7f3";
  ctx.fill();
  ctx.restore();

  ctx.save();
  roundRectPath(ctx, cardX, cardY, cardW, cardH, 28);
  ctx.clip();
  ctx.imageSmoothingQuality = "high";
  ctx.drawImage(img, cardX, cardY, cardW, cardH);
  ctx.restore();

  ctx.fillStyle = "#aa6e76";
  ctx.textAlign = "center";
  ctx.textBaseline = "alphabetic";
  ctx.font = '600 30px "Pretendard Variable", Pretendard, system-ui, sans-serif';
  ctx.fillText("나의 공주 엔딩", W / 2, cardY - 50);

  ctx.fillStyle = "#1d1a17";
  ctx.font = '700 46px "Pretendard Variable", Pretendard, system-ui, sans-serif';
  ctx.fillText("Princess Project", W / 2, cardY + cardH + 110);

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
