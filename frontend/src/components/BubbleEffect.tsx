import { useMemo } from "react";

interface Bubble {
  key: number;
  left: number;
  size: number;
  duration: number;
  delay: number;
  drift: number;
}

function makeBubbles(count: number): Bubble[] {
  return Array.from({ length: count }, (_, i) => ({
    key: i,
    left: Math.random() * 100,
    size: 10 + Math.random() * 26,
    duration: 10 + Math.random() * 10,
    delay: Math.random() * 14,
    drift: (Math.random() - 0.5) * 60,
  }));
}

export function BubbleEffect() {
  const bubbles = useMemo(() => makeBubbles(18), []);

  return (
    <div className="bubble-layer" aria-hidden="true">
      {bubbles.map((b) => (
        <span
          key={b.key}
          className="bubble"
          style={
            {
              left: `${b.left}%`,
              width: b.size,
              height: b.size,
              animationDuration: `${b.duration}s`,
              animationDelay: `${-b.delay}s`,
              "--drift": `${b.drift}px`,
            } as React.CSSProperties
          }
        />
      ))}
    </div>
  );
}
