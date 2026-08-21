import { useEffect, useState } from "react";
import { ApiError } from "../api/client";
import { getDailyCommonTasks, getWeeklyCommonTask, saveCommonTask } from "../api/endpoints";
import type { CommonTaskResponse } from "../api/types";
import { useToast } from "./ToastProvider";

// A "field to write these in" was missing from the whole app even though the setup wizard's
// notice above the goal list calls 독서/공부/주간회고 mandatory for every participant (전원
// 필수) - this card is that field. It's rendered on RecordPage alongside the regular mission
// list, but intentionally isn't a UserMission: these apply no matter which 아비투스/capitals
// someone picked, so they can't live inside the weighted goal/stat tree without either
// force-attaching a goal nobody chose or being invisible to anyone who skipped 지식.

const MAX_PAGE_NUMBER = 100000;
const MAX_PAGES_PER_DAY = 2000;
const MAX_STUDY_AMOUNT = 100000;
const MAX_RETRO_TEXT_LENGTH = 5000;

function todayIso(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function commonTaskErrorMessage(err: unknown, fallback: string): string {
  if (!(err instanceof ApiError) || !err.code) return fallback;
  const messages: Record<string, string> = {
    READING_PAGES_REQUIRED: "시작 페이지와 종료 페이지를 모두 입력해주세요.",
    READING_PAGE_OUT_OF_RANGE: `페이지 번호는 0~${MAX_PAGE_NUMBER.toLocaleString()} 사이여야 해요.`,
    READING_END_BEFORE_START: "종료 페이지가 시작 페이지보다 앞설 수 없어요.",
    READING_RANGE_TOO_LARGE: `하루 독서량이 너무 커요. ${MAX_PAGES_PER_DAY.toLocaleString()}p 이하로 입력해주세요.`,
    STUDY_COMPLETED_REQUIRED: "오늘 완료량을 입력해주세요.",
    STUDY_COMPLETED_OUT_OF_RANGE: `완료량은 0~${MAX_STUDY_AMOUNT.toLocaleString()} 사이여야 해요.`,
    STUDY_PLANNED_OUT_OF_RANGE: `계획량은 0~${MAX_STUDY_AMOUNT.toLocaleString()} 사이여야 해요.`,
    RETROSPECTIVE_EMPTY: "PART1~3 중 최소 하나는 내용을 입력해주세요.",
    RETROSPECTIVE_TOO_LONG: `각 항목은 ${MAX_RETRO_TEXT_LENGTH.toLocaleString()}자 이하로 입력해주세요.`,
  };
  return messages[err.code] ?? fallback;
}

function ReadingSection() {
  const { showToast } = useToast();
  const [existing, setExisting] = useState<CommonTaskResponse | null>(null);
  const [startPage, setStartPage] = useState("");
  const [endPage, setEndPage] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDailyCommonTasks(todayIso())
      .then((entries) => setExisting(entries.find((e) => e.taskType === "READING") ?? null))
      .catch(() => {
        // A failed load here just means the section starts as "not recorded yet" - the
        // save call below will still work, and a retry naturally happens on next visit.
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    const start = Number(startPage);
    const end = Number(endPage);
    if (startPage === "" || endPage === "" || Number.isNaN(start) || Number.isNaN(end)) {
      setError("시작 페이지와 종료 페이지를 모두 입력해주세요.");
      return;
    }
    if (start < 0 || end < 0 || start > MAX_PAGE_NUMBER || end > MAX_PAGE_NUMBER) {
      setError(`페이지 번호는 0~${MAX_PAGE_NUMBER.toLocaleString()} 사이여야 해요.`);
      return;
    }
    if (end < start) {
      setError("종료 페이지가 시작 페이지보다 앞설 수 없어요.");
      return;
    }
    if (end - start > MAX_PAGES_PER_DAY) {
      setError(`하루 독서량이 너무 커요. ${MAX_PAGES_PER_DAY.toLocaleString()}p 이하로 입력해주세요.`);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const saved = await saveCommonTask({ taskType: "READING", date: todayIso(), startPage: start, endPage: end });
      setExisting(saved);
      showToast("독서 기록이 저장되었어요");
    } catch (err) {
      setError(commonTaskErrorMessage(err, "저장에 실패했습니다. 다시 시도해주세요."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return null;

  if (existing) {
    return (
      <div className="stack" style={{ gap: 6 }}>
        <strong style={{ fontSize: 13.5 }}>독서</strong>
        <p className="muted" style={{ margin: 0, fontSize: 13 }}>
          오늘 {existing.startPage}p ~ {existing.endPage}p ({(existing.endPage ?? 0) - (existing.startPage ?? 0)}p) 기록 완료
        </p>
      </div>
    );
  }

  return (
    <div className="stack" style={{ gap: 6 }}>
      <strong style={{ fontSize: 13.5 }}>독서</strong>
      <p className="muted" style={{ margin: 0, fontSize: 12.5 }}>
        오늘 읽은 시작~종료 페이지를 기록해요. 일일 최소 권장 10p
      </p>
      <div className="row" style={{ gap: 6 }}>
        <input
          type="number"
          min={0}
          max={MAX_PAGE_NUMBER}
          placeholder="시작 페이지"
          value={startPage}
          onChange={(e) => setStartPage(e.target.value)}
          style={{ maxWidth: 110 }}
        />
        <span className="muted">~</span>
        <input
          type="number"
          min={0}
          max={MAX_PAGE_NUMBER}
          placeholder="종료 페이지"
          value={endPage}
          onChange={(e) => setEndPage(e.target.value)}
          style={{ maxWidth: 110 }}
        />
        <button type="button" className="primary" onClick={handleSave} disabled={saving} style={{ padding: "6px 12px" }}>
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
      {error && <div className="error-banner">{error}</div>}
    </div>
  );
}

function StudySection() {
  const { showToast } = useToast();
  const [existing, setExisting] = useState<CommonTaskResponse | null>(null);
  const [planned, setPlanned] = useState("");
  const [completed, setCompleted] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDailyCommonTasks(todayIso())
      .then((entries) => setExisting(entries.find((e) => e.taskType === "STUDY") ?? null))
      .catch(() => {
        // See ReadingSection's identical catch - starts as "not recorded yet" on failure.
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    const completedValue = Number(completed);
    if (completed === "" || Number.isNaN(completedValue)) {
      setError("오늘 완료량을 입력해주세요.");
      return;
    }
    if (completedValue < 0 || completedValue > MAX_STUDY_AMOUNT) {
      setError(`완료량은 0~${MAX_STUDY_AMOUNT.toLocaleString()} 사이여야 해요.`);
      return;
    }
    let plannedValue: number | undefined;
    if (planned !== "") {
      plannedValue = Number(planned);
      if (Number.isNaN(plannedValue) || plannedValue < 0 || plannedValue > MAX_STUDY_AMOUNT) {
        setError(`계획량은 0~${MAX_STUDY_AMOUNT.toLocaleString()} 사이여야 해요.`);
        return;
      }
    }
    setSaving(true);
    setError(null);
    try {
      const saved = await saveCommonTask({
        taskType: "STUDY",
        date: todayIso(),
        studyCompletedAmount: completedValue,
        studyPlannedAmount: plannedValue,
      });
      setExisting(saved);
      showToast("공부 기록이 저장되었어요");
    } catch (err) {
      setError(commonTaskErrorMessage(err, "저장에 실패했습니다. 다시 시도해주세요."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return null;

  if (existing) {
    return (
      <div className="stack" style={{ gap: 6 }}>
        <strong style={{ fontSize: 13.5 }}>공부</strong>
        <p className="muted" style={{ margin: 0, fontSize: 13 }}>
          오늘 완료량 {existing.studyCompletedAmount}
          {existing.studyPlannedAmount != null && ` / 계획량 ${existing.studyPlannedAmount}`} 기록 완료
        </p>
      </div>
    );
  }

  return (
    <div className="stack" style={{ gap: 6 }}>
      <strong style={{ fontSize: 13.5 }}>공부</strong>
      <p className="muted" style={{ margin: 0, fontSize: 12.5 }}>
        주간 계획량을 미리 정해두고, 오늘 완료량을 기록해요 (측정이 어려우면 시간 기준도 가능해요)
      </p>
      <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
        <input
          type="number"
          min={0}
          max={MAX_STUDY_AMOUNT}
          placeholder="이번 주 계획량 (선택)"
          value={planned}
          onChange={(e) => setPlanned(e.target.value)}
          style={{ maxWidth: 150 }}
        />
        <input
          type="number"
          min={0}
          max={MAX_STUDY_AMOUNT}
          placeholder="오늘 완료량"
          value={completed}
          onChange={(e) => setCompleted(e.target.value)}
          style={{ maxWidth: 120 }}
        />
        <button type="button" className="primary" onClick={handleSave} disabled={saving} style={{ padding: "6px 12px" }}>
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
      {error && <div className="error-banner">{error}</div>}
    </div>
  );
}

function WeeklyRetrospectiveSection() {
  const { showToast } = useToast();
  const [dailyLife, setDailyLife] = useState("");
  const [weekReview, setWeekReview] = useState("");
  const [nextWeekPlan, setNextWeekPlan] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastSavedAt, setLastSavedAt] = useState<string | null>(null);

  useEffect(() => {
    getWeeklyCommonTask(todayIso())
      .then((existing) => {
        if (existing) {
          setDailyLife(existing.retroDailyLife ?? "");
          setWeekReview(existing.retroWeekReview ?? "");
          setNextWeekPlan(existing.retroNextWeekPlan ?? "");
          setLastSavedAt(existing.recordDate);
        }
      })
      .catch(() => {
        // No existing entry (or a failed load) just leaves the 3 fields blank/editable.
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    if (!dailyLife.trim() && !weekReview.trim() && !nextWeekPlan.trim()) {
      setError("PART1~3 중 최소 하나는 내용을 입력해주세요.");
      return;
    }
    if (
      dailyLife.length > MAX_RETRO_TEXT_LENGTH ||
      weekReview.length > MAX_RETRO_TEXT_LENGTH ||
      nextWeekPlan.length > MAX_RETRO_TEXT_LENGTH
    ) {
      setError(`각 항목은 ${MAX_RETRO_TEXT_LENGTH.toLocaleString()}자 이하로 입력해주세요.`);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const saved = await saveCommonTask({
        taskType: "WEEKLY_RETROSPECTIVE",
        date: todayIso(),
        retroDailyLife: dailyLife.trim() || undefined,
        retroWeekReview: weekReview.trim() || undefined,
        retroNextWeekPlan: nextWeekPlan.trim() || undefined,
      });
      setLastSavedAt(saved.recordDate);
      showToast("주간 회고가 저장되었어요");
    } catch (err) {
      setError(commonTaskErrorMessage(err, "저장에 실패했습니다. 다시 시도해주세요."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return null;

  return (
    <div className="stack" style={{ gap: 6 }}>
      <strong style={{ fontSize: 13.5 }}>주간 회고{lastSavedAt && " (이번 주 작성됨 · 수정 가능)"}</strong>
      <div className="stack" style={{ gap: 8 }}>
        <div className="stack" style={{ gap: 4 }}>
          <label className="muted" style={{ fontSize: 12.5 }}>
            PART1. 일상 공유
          </label>
          <textarea value={dailyLife} onChange={(e) => setDailyLife(e.target.value)} rows={2} maxLength={MAX_RETRO_TEXT_LENGTH} />
        </div>
        <div className="stack" style={{ gap: 4 }}>
          <label className="muted" style={{ fontSize: 12.5 }}>
            PART2. 이번 주 회고
          </label>
          <textarea value={weekReview} onChange={(e) => setWeekReview(e.target.value)} rows={2} maxLength={MAX_RETRO_TEXT_LENGTH} />
        </div>
        <div className="stack" style={{ gap: 4 }}>
          <label className="muted" style={{ fontSize: 12.5 }}>
            PART3. 다음 주 계획
          </label>
          <textarea value={nextWeekPlan} onChange={(e) => setNextWeekPlan(e.target.value)} rows={2} maxLength={MAX_RETRO_TEXT_LENGTH} />
        </div>
        <button
          type="button"
          className="primary"
          onClick={handleSave}
          disabled={saving}
          style={{ alignSelf: "flex-start", padding: "6px 14px" }}
        >
          {saving ? "저장 중..." : lastSavedAt ? "수정 저장" : "저장"}
        </button>
      </div>
      {error && <div className="error-banner">{error}</div>}
    </div>
  );
}

export function CommonTasksCard() {
  return (
    <div className="card stack" style={{ gap: 16 }}>
      <div>
        <span className="badge good">공통 과제 3종</span>
        <p className="muted" style={{ margin: "6px 0 0", fontSize: 12.5 }}>
          어떤 아비투스를 골랐든 모든 참가자가 함께 하는 과제예요.
        </p>
      </div>
      <ReadingSection />
      <StudySection />
      <WeeklyRetrospectiveSection />
    </div>
  );
}
