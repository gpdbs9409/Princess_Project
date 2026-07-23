import { useEffect, useRef, useState } from "react";

// Trails the cursor with easing rather than snapping straight to it, so it
// reads as "following" instead of just relocating instantly on every move.
const EASING = 0.12;
const OFFSET_X = 24;
const OFFSET_Y = 24;

export function Mascot() {
  const elRef = useRef<HTMLImageElement>(null);
  const target = useRef({ x: window.innerWidth / 2, y: window.innerHeight / 2 });
  const current = useRef({ x: window.innerWidth / 2, y: window.innerHeight / 2 });
  const [loaded, setLoaded] = useState(true);

  useEffect(() => {
    const handleMove = (e: MouseEvent) => {
      target.current = { x: e.clientX + OFFSET_X, y: e.clientY + OFFSET_Y };
    };
    window.addEventListener("mousemove", handleMove);

    let frame: number;
    const tick = () => {
      current.current.x += (target.current.x - current.current.x) * EASING;
      current.current.y += (target.current.y - current.current.y) * EASING;
      const el = elRef.current;
      if (el) {
        el.style.transform = `translate3d(${current.current.x}px, ${current.current.y}px, 0)`;
      }
      frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);

    return () => {
      window.removeEventListener("mousemove", handleMove);
      cancelAnimationFrame(frame);
    };
  }, []);

  if (!loaded) return null;

  return (
    <img
      ref={elRef}
      src="/mascot/character.png"
      alt=""
      className="mascot-follower"
      onError={() => setLoaded(false)}
    />
  );
}
