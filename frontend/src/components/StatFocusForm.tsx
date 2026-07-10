import { useState } from "react";
import { STAT_LABELS, STAT_TYPES, type StatType } from "../api/types";

interface StatFocusFormProps {
  initial: Partial<Record<StatType, number>>;
  submitLabel: string;
  onSubmit: (stats: { statType: StatType; weightPercent: number }[]) => Promise<void>;
}

export function StatFocusForm({ initial, submitLabel, onSubmit }: StatFocusFormProps) {
  const [weights, setWeights] = useState<Record<StatType, number>>(() => {
    const base = {} as Record<StatType, number>;
    STAT_TYPES.forEach((s) => {
      base[s] = initial[s] ?? 0;
    });
    return base;
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const total = STAT_TYPES.reduce((sum, s) => sum + (weights[s] || 0), 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const selected = STAT_TYPES.filter((s) => weights[s] > 0).map((s) => ({
      statType: s,
      weightPercent: weights[s],
    }));
    if (selected.length === 0) {
      setError("최소 하나의 스탯에는 비중을 입력해주세요.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit(selected);
    } catch {
      setError("저장에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="stack" onSubmit={handleSubmit}>
      <p className="muted">
        중요하게 키우고 싶은 스탯에 비중(%)을 입력하세요. 비중이 0보다 큰 스탯만 저장됩니다.
      </p>
      <div className="stack" style={{ gap: 10 }}>
        {STAT_TYPES.map((statType) => (
          <div className="row-between" key={statType}>
            <label style={{ minWidth: 60 }}>{STAT_LABELS[statType]}</label>
            <input
              type="number"
              min={0}
              max={100}
              value={weights[statType]}
              onChange={(e) =>
                setWeights((w) => ({ ...w, [statType]: Number(e.target.value) || 0 }))
              }
              style={{ maxWidth: 100 }}
            />
          </div>
        ))}
      </div>
      <div className="row-between">
        <span className="muted">합계: {total}%</span>
      </div>
      {error && <div className="error-banner">{error}</div>}
      <button type="submit" className="primary" disabled={submitting}>
        {submitting ? "저장 중..." : submitLabel}
      </button>
    </form>
  );
}
