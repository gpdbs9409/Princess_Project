import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ApiError } from "../api/client";
import {
  addRecruitmentApplicant,
  addRecruitmentApplicantsBulk,
  assignAdminCohort,
  clearAdminMvp,
  deleteRecruitmentApplicant,
  getAdminActivitiesForReview,
  getAdminApplicants,
  getAdminCohorts,
  getAdminMemberActivities,
  getAdminParticipants,
  getRecruitmentApplicants,
  setAdminMvp,
  setAdminActivityInvalidated,
  setAdminRefundPaid,
  updateRecruitmentApplicant,
} from "../api/endpoints";
import type {
  AdminActivityResponse,
  AdminApplicantResponse,
  AdminMemberWeekResponse,
  RecruitmentApplicantRequest,
  RecruitmentApplicantResponse,
  RecruitmentStatus,
} from "../api/types";
import { parseRecruitmentCsv } from "../lib/parseRecruitmentCsv";

type Tab = "participants" | "unassigned" | "recruitment";
type ParticipantFilter = "ALL" | "NEEDS_ATTENTION" | "ELIGIBLE" | "UNPAID" | "PAID";
const DAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];

const RECRUITMENT_STATUS_LABELS: Record<RecruitmentStatus, string> = {
  PENDING: "검토중",
  ACCEPTED: "합격",
  REJECTED: "불합격",
};

function blankRecruitmentForm() {
  return { name: "", contact: "", note: "", status: "PENDING" as RecruitmentStatus };
}

function mondayOf(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay(); // 0 = Sun
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

function isoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

/** 2026-08-29 -> "8월 29일 (금)" */
function formatDayLabel(iso: string): string {
  const d = new Date(`${iso}T00:00:00`);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${WEEKDAY_LABELS[d.getDay()]})`;
}

function downloadCsv(filename: string, rows: (string | number)[][]) {
  const csv = rows
    .map((row) =>
      row
        .map((cell) => {
          const text = String(cell ?? "");
          return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
        })
        .join(",")
    )
    .join("\n");
  // Excel opens UTF-8 CSVs correctly only with a BOM prefix - otherwise Korean text garbles.
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function AdminPage() {
  const [tab, setTab] = useState<Tab>("participants");
  const [cohorts, setCohorts] = useState<string[]>([]);
  const [selectedCohort, setSelectedCohort] = useState<string>("");
  const [weekStart, setWeekStart] = useState<Date>(() => mondayOf(new Date()));
  const [applicants, setApplicants] = useState<AdminApplicantResponse[]>([]);
  const [participants, setParticipants] = useState<AdminMemberWeekResponse[]>([]);
  const [participantFilter, setParticipantFilter] = useState<ParticipantFilter>("ALL");
  const [participantSearch, setParticipantSearch] = useState("");
  const [activityMember, setActivityMember] = useState<AdminMemberWeekResponse | null>(null);
  const [openActivityDates, setOpenActivityDates] = useState<string[]>([]);
  const [activities, setActivities] = useState<AdminActivityResponse[]>([]);
  const [activitiesLoading, setActivitiesLoading] = useState(false);
  const [activitiesError, setActivitiesError] = useState<string | null>(null);
  const [reviewActivities, setReviewActivities] = useState<AdminActivityResponse[]>([]);
  const [reviewOpen, setReviewOpen] = useState(false);
  const [newCohortInput, setNewCohortInput] = useState<Record<number, string>>({});
  // "직접 입력(새 기수)"를 고른 행만 true - 그 외에는 드롭다운으로 기존 기수 중에서만 고르게
  // 해서, 오타로 잘못된 기수 문자열이 만들어지는 걸 막는다 (2026-08-26 요청: 플레인텍스트
  // 입력은 위험하니 드롭다운으로).
  const [customCohortRows, setCustomCohortRows] = useState<Record<number, boolean>>({});
  const NEW_COHORT_OPTION = "__new__";
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [recruitmentApplicants, setRecruitmentApplicants] = useState<RecruitmentApplicantResponse[]>([]);
  const [recruitmentForm, setRecruitmentForm] = useState(blankRecruitmentForm());
  const [csvUploading, setCsvUploading] = useState(false);
  const [csvMessage, setCsvMessage] = useState<string | null>(null);
  const [pendingCsv, setPendingCsv] = useState<{
    rows: RecruitmentApplicantRequest[];
    skipped: number;
    fileName: string;
  } | null>(null);
  const csvInputRef = useRef<HTMLInputElement>(null);

  // 날짜별로 한 단계 접어서 보여준다. 평평한 목록은 30일치가 쌓이면 훑기가 어려워서,
  // 어느 날 무엇을 했는지 날짜 단위로 먼저 보이게 한다.
  const activitiesByDate = useMemo(() => {
    const grouped = new Map<string, AdminActivityResponse[]>();
    for (const activity of activities) {
      const list = grouped.get(activity.recordDate);
      if (list) list.push(activity);
      else grouped.set(activity.recordDate, [activity]);
    }
    return [...grouped.entries()].sort((a, b) => b[0].localeCompare(a[0]));
  }, [activities]);

  const weekStartIso = useMemo(() => isoDate(weekStart), [weekStart]);

  const participantSummary = useMemo(() => ({
    total: participants.length,
    eligible: participants.filter((p) => p.eligible).length,
    unpaid: participants.filter((p) => p.eligible && !p.paid).length,
    paid: participants.filter((p) => p.paid).length,
    needsAttention: participants.filter((p) => !p.eligible).length,
  }), [participants]);

  const photoReviewUserIds = useMemo(
    () => new Set(reviewActivities.filter((activity) => !activity.adminInvalidated).map((activity) => activity.userId)),
    [reviewActivities]
  );

  const visibleParticipants = useMemo(() => {
    const query = participantSearch.trim().toLowerCase();
    return participants
      .filter((p) => !query || p.nickname.toLowerCase().includes(query))
      .filter((p) => {
        if (participantFilter === "ELIGIBLE") return p.eligible;
        if (participantFilter === "UNPAID") return p.eligible && !p.paid;
        if (participantFilter === "PAID") return p.paid;
        if (participantFilter === "NEEDS_ATTENTION") return !p.eligible;
        return true;
      })
      .sort((a, b) => {
        if (a.paid !== b.paid) return a.paid ? 1 : -1;
        if (a.eligible !== b.eligible) return a.eligible ? -1 : 1;
        return b.successDays - a.successDays || a.nickname.localeCompare(b.nickname, "ko");
      });
  }, [participants, participantFilter, participantSearch]);

  const loadCohorts = useCallback(async () => {
    try {
      const list = await getAdminCohorts();
      setCohorts(list);
    } catch {
      // non-fatal - the tab still works without the filter list
    }
  }, []);

  const loadApplicants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setApplicants(await getAdminApplicants());
    } catch {
      setError("목록을 불러오지 못했어요.");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadRecruitmentApplicants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setRecruitmentApplicants(await getRecruitmentApplicants());
    } catch {
      setError("지원서 목록을 불러오지 못했어요.");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadParticipants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [memberList, reviewList] = await Promise.all([
        getAdminParticipants(selectedCohort || null, weekStartIso),
        getAdminActivitiesForReview(selectedCohort || null),
      ]);
      setParticipants(memberList);
      setReviewActivities(reviewList);
    } catch {
      setError("참가자 목록을 불러오지 못했어요. 관리자 권한이 있는 계정인지 확인해주세요.");
    } finally {
      setLoading(false);
    }
  }, [selectedCohort, weekStartIso]);

  useEffect(() => {
    loadCohorts();
  }, [loadCohorts]);

  useEffect(() => {
    if (tab === "unassigned") loadApplicants();
    else if (tab === "recruitment") loadRecruitmentApplicants();
    else loadParticipants();
  }, [tab, loadApplicants, loadParticipants, loadRecruitmentApplicants]);

  const handleAssignCohort = async (userId: number, cohort: string) => {
    if (!cohort.trim()) return;
    await assignAdminCohort(userId, cohort.trim());
    await Promise.all([loadCohorts(), loadApplicants()]);
  };

  const openActivities = async (member: AdminMemberWeekResponse) => {
    setActivityMember(member);
    setActivities([]);
    setOpenActivityDates([]);
    setActivitiesError(null);
    setActivitiesLoading(true);
    try {
      const loaded = await getAdminMemberActivities(member.userId);
      setActivities(loaded);
      // 가장 최근 날짜 하나만 펼쳐둔다 - 전부 접혀 있으면 한 번 더 눌러야 해서 번거롭고,
      // 전부 펼치면 30일치가 쏟아져 원래 문제로 돌아간다.
      const latest = loaded.reduce<string | null>(
        (max, a) => (max === null || a.recordDate > max ? a.recordDate : max), null);
      setOpenActivityDates(latest ? [latest] : []);
    } catch {
      setActivitiesError("수행 내역을 불러오지 못했어요.");
    } finally {
      setActivitiesLoading(false);
    }
  };

  const handleAddRecruitmentApplicant = async () => {
    if (!recruitmentForm.name.trim()) return;
    const created = await addRecruitmentApplicant({
      name: recruitmentForm.name.trim(),
      contact: recruitmentForm.contact || undefined,
      note: recruitmentForm.note || undefined,
      status: recruitmentForm.status,
    });
    setRecruitmentApplicants((prev) => [created, ...prev]);
    setRecruitmentForm(blankRecruitmentForm());
  };

  const handleDownloadCsvTemplate = () => {
    // BOM first, otherwise Excel opens the Korean headers as mojibake.
    const csv = "﻿이름,연락처,메모,상태,신청일\n김프린,010-0000-0000,인스타 DM 문의,검토중,2026-08-01\n";
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8;" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "지원서_양식.csv";
    link.click();
    URL.revokeObjectURL(url);
  };

  const resetCsvInput = () => {
    if (csvInputRef.current) csvInputRef.current.value = "";
  };

  // Parsing and uploading are deliberately split: picking a file only previews what was
  // read, so an operator can back out before writing rows they didn't mean to import.
  const handleCsvFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setCsvMessage(null);
    try {
      const { rows, skipped } = parseRecruitmentCsv(await file.text());
      if (rows.length === 0) {
        setCsvMessage("등록할 행을 찾지 못했어요. 첫 번째 열에 이름이 있는지 확인해주세요.");
        resetCsvInput();
        return;
      }
      setPendingCsv({ rows, skipped, fileName: file.name });
    } catch {
      setCsvMessage("파일을 읽지 못했어요. CSV UTF-8로 저장했는지 확인해주세요.");
      resetCsvInput();
    }
  };

  const handleCancelCsvUpload = () => {
    setPendingCsv(null);
    resetCsvInput();
  };

  const handleConfirmCsvUpload = async () => {
    if (!pendingCsv) return;
    setCsvUploading(true);
    try {
      const created = await addRecruitmentApplicantsBulk(pendingCsv.rows);
      setRecruitmentApplicants((prev) => [...created.slice().reverse(), ...prev]);
      setCsvMessage(
        `${created.length}건을 등록했어요.` +
          (pendingCsv.skipped > 0 ? ` 이름이 비어 있는 ${pendingCsv.skipped}행은 건너뛰었어요.` : "")
      );
      setPendingCsv(null);
    } catch {
      setCsvMessage("등록에 실패했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setCsvUploading(false);
      resetCsvInput();
    }
  };

  const handleRecruitmentStatusChange = async (applicant: RecruitmentApplicantResponse, status: RecruitmentStatus) => {
    const updated = await updateRecruitmentApplicant(applicant.id, {
      name: applicant.name,
      contact: applicant.contact ?? undefined,
      note: applicant.note ?? undefined,
      status,
      appliedAt: applicant.appliedAt ?? undefined,
    });
    setRecruitmentApplicants((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
  };

  const handleDeleteRecruitmentApplicant = async (id: number) => {
    await deleteRecruitmentApplicant(id);
    setRecruitmentApplicants((prev) => prev.filter((a) => a.id !== id));
  };

  const handleTogglePaid = async (member: AdminMemberWeekResponse) => {
    const updated = await setAdminRefundPaid(member.userId, weekStartIso, !member.paid);
    setParticipants((prev) => prev.map((p) => (p.userId === updated.userId ? updated : p)));
  };

  const handleActivityInvalidation = async (activity: AdminActivityResponse) => {
    const invalidated = !activity.adminInvalidated;
    await setAdminActivityInvalidated(activity.activityType, activity.id, invalidated);
    setReviewActivities((items) => items.map((item) =>
      item.id === activity.id && item.activityType === activity.activityType
        ? { ...item, adminInvalidated: invalidated }
        : item
    ));
    setActivities((items) => items.map((item) =>
      item.id === activity.id && item.activityType === activity.activityType
        ? { ...item, adminInvalidated: invalidated }
        : item
    ));
    await loadParticipants();
  };

  const handleToggleMvp = async (member: AdminMemberWeekResponse) => {
    if (!member.cohort) return;
    try {
      if (member.isMvp) {
        await clearAdminMvp(member.cohort, weekStartIso);
      } else {
        await setAdminMvp(member.userId, weekStartIso);
      }
      await loadParticipants();
    } catch (err) {
      // 1인 1회 제한 위반 등 - 서버가 이유를 메시지로 내려줌 (주간 MVP 정책 v1.0, 2026-08-20)
      alert(err instanceof ApiError ? err.message : "MVP 지정에 실패했습니다.");
    }
  };




  const shiftWeek = (deltaDays: number) => {
    const next = new Date(weekStart);
    next.setDate(next.getDate() + deltaDays);
    setWeekStart(mondayOf(next));
  };

  const exportParticipantsCsv = () => {
    const rows: (string | number)[][] = [
      ["닉네임", "기수", "주 시작일", "주 종료일", "성공일수(7일 중)", "환급 대상", "환급 지급여부"],
      ...participants.map((p) => [
        p.nickname,
        p.cohort ?? "",
        p.weekStart,
        p.weekEnd,
        p.successDays,
        p.eligible ? "대상" : "미대상",
        p.paid ? "지급완료" : "미지급",
      ]),
    ];
    downloadCsv(`princess-project_${selectedCohort || "전체"}_${weekStartIso}.csv`, rows);
  };

  return (
    <div className="container">
      <div className="stack" style={{ marginBottom: 20 }}>
        <span className="eyebrow">Admin</span>
        <h1 style={{ fontSize: 26 }}>회원 관리</h1>
      </div>

      <div className="row" style={{ gap: 10, marginBottom: 16 }}>
        <button
          type="button"
          className={tab === "participants" ? "primary" : "ghost"}
          onClick={() => setTab("participants")}
        >
          참가자 (기수별 주간 관리)
        </button>
        <button
          type="button"
          className={tab === "unassigned" ? "primary" : "ghost"}
          onClick={() => setTab("unassigned")}
        >
          기수 미배정 회원
        </button>
        <button
          type="button"
          className={tab === "recruitment" ? "primary" : "ghost"}
          onClick={() => setTab("recruitment")}
        >
          지원서 (내부 기록용)
        </button>
      </div>

      {error && <div className="error-banner" style={{ marginBottom: 16 }}>{error}</div>}

      {tab === "unassigned" && (
        <div className="card">
          <p className="muted" style={{ marginTop: 0 }}>
            이미 회원가입한, 아직 기수 태그만 없는 회원이에요. 이 목록의 "지원서(내부
            기록용)" 탭과는 무관해요 - 저건 회원가입 전 단계의 모집 기록이에요.
          </p>
          {loading && <p className="muted">불러오는 중...</p>}
          {!loading && applicants.length === 0 && <p className="muted">기수 미배정 회원이 없어요.</p>}
          {!loading && applicants.length > 0 && (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>닉네임</th>
                  <th>신청일</th>
                  <th>기수 배정</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {applicants.map((a) => (
                  <tr key={a.userId}>
                    <td>{a.nickname}</td>
                    <td>{new Date(a.appliedAt).toLocaleDateString("ko-KR")}</td>
                    <td>
                      <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                        {customCohortRows[a.userId] ? (
                          <input
                            type="text"
                            placeholder="예: 1기"
                            style={{ maxWidth: 100 }}
                            value={newCohortInput[a.userId] ?? ""}
                            onChange={(e) =>
                              setNewCohortInput((prev) => ({ ...prev, [a.userId]: e.target.value }))
                            }
                          />
                        ) : (
                          <select
                            style={{ maxWidth: 130 }}
                            value={newCohortInput[a.userId] ?? ""}
                            onChange={(e) => {
                              const value = e.target.value;
                              if (value === NEW_COHORT_OPTION) {
                                setCustomCohortRows((prev) => ({ ...prev, [a.userId]: true }));
                                setNewCohortInput((prev) => ({ ...prev, [a.userId]: "" }));
                              } else {
                                setNewCohortInput((prev) => ({ ...prev, [a.userId]: value }));
                              }
                            }}
                          >
                            <option value="">기수 선택</option>
                            {cohorts.map((c) => (
                              <option key={c} value={c}>
                                {c}
                              </option>
                            ))}
                            <option value={NEW_COHORT_OPTION}>+ 새 기수 직접 입력</option>
                          </select>
                        )}
                        {customCohortRows[a.userId] && (
                          <button
                            type="button"
                            className="ghost"
                            style={{ padding: "4px 8px", fontSize: 12 }}
                            onClick={() => {
                              setCustomCohortRows((prev) => ({ ...prev, [a.userId]: false }));
                              setNewCohortInput((prev) => ({ ...prev, [a.userId]: "" }));
                            }}
                          >
                            목록에서 선택
                          </button>
                        )}
                        <button
                          type="button"
                          className="ghost"
                          onClick={() => handleAssignCohort(a.userId, newCohortInput[a.userId] ?? "")}
                        >
                          배정
                        </button>
                      </div>
                    </td>
                    <td>{a.role === "ADMIN" && <span className="role-tag">관리자</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {tab === "recruitment" && (
        <div className="stack" style={{ gap: 12 }}>
          <div className="card">
            <p className="muted" style={{ marginTop: 0 }}>
              앱 회원(users)과 무관한, 팀 내부 모집 기록용 리스트예요.
            </p>
            <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
              <input
                type="text"
                placeholder="이름"
                value={recruitmentForm.name}
                onChange={(e) => setRecruitmentForm((f) => ({ ...f, name: e.target.value }))}
                style={{ maxWidth: 140 }}
              />
              <input
                type="text"
                placeholder="연락처 (전화/인스타 등)"
                value={recruitmentForm.contact}
                onChange={(e) => setRecruitmentForm((f) => ({ ...f, contact: e.target.value }))}
                style={{ maxWidth: 180 }}
              />
              <select
                value={recruitmentForm.status}
                onChange={(e) =>
                  setRecruitmentForm((f) => ({ ...f, status: e.target.value as RecruitmentStatus }))
                }
                style={{ maxWidth: 110 }}
              >
                {(Object.keys(RECRUITMENT_STATUS_LABELS) as RecruitmentStatus[]).map((s) => (
                  <option key={s} value={s}>
                    {RECRUITMENT_STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
              <input
                type="text"
                placeholder="메모"
                value={recruitmentForm.note}
                onChange={(e) => setRecruitmentForm((f) => ({ ...f, note: e.target.value }))}
                style={{ flex: 1, minWidth: 160 }}
              />
              <button type="button" className="primary" onClick={handleAddRecruitmentApplicant}>
                추가
              </button>
            </div>
          </div>

          <div className="card">
            <div className="row-between" style={{ flexWrap: "wrap", gap: 8 }}>
              <strong style={{ fontSize: 14 }}>엑셀(CSV) 일괄 등록</strong>
              <button type="button" className="ghost" onClick={handleDownloadCsvTemplate}>
                양식 내려받기
              </button>
            </div>
            <p className="muted" style={{ fontSize: 13 }}>
              열 순서: <strong>이름 · 연락처 · 메모 · 상태 · 신청일</strong> · 이름만 필수예요.
              엑셀에서 <strong>다른 이름으로 저장 → CSV UTF-8</strong>로 저장한 뒤 올려주세요.
              상태는 검토중/합격/불합격으로 쓰면 되고, 비워두면 검토중으로 들어가요.
            </p>
            <div className="row" style={{ gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <input
                ref={csvInputRef}
                type="file"
                accept=".csv,text/csv"
                onChange={handleCsvFileChange}
                disabled={csvUploading}
              />
            </div>

            {pendingCsv && (
              <div className="modal-overlay">
                <div className="modal-card stack" style={{ gap: 12, textAlign: "left" }}>
                  <h2 style={{ fontSize: 18, margin: 0 }}>지원서를 등록할까요?</h2>
                  <p className="muted" style={{ margin: 0 }}>
                    <strong>{pendingCsv.fileName}</strong>에서 <strong>{pendingCsv.rows.length}건</strong>을
                    읽었어요.
                    {pendingCsv.skipped > 0 && ` 이름이 비어 있는 ${pendingCsv.skipped}행은 제외돼요.`}
                  </p>
                  <ul className="muted" style={{ margin: 0, paddingLeft: 18, fontSize: 13 }}>
                    {pendingCsv.rows.slice(0, 5).map((r, i) => (
                      <li key={i}>
                        {r.name}
                        {r.contact ? ` · ${r.contact}` : ""}
                      </li>
                    ))}
                    {pendingCsv.rows.length > 5 && <li>외 {pendingCsv.rows.length - 5}건</li>}
                  </ul>
                  <div className="row" style={{ gap: 8, justifyContent: "flex-end" }}>
                    <button type="button" className="ghost" onClick={handleCancelCsvUpload} disabled={csvUploading}>
                      아니오
                    </button>
                    <button type="button" className="primary" onClick={handleConfirmCsvUpload} disabled={csvUploading}>
                      {csvUploading ? "등록 중..." : "예, 등록할게요"}
                    </button>
                  </div>
                </div>
              </div>
            )}
            {csvMessage && (
              <p className="muted" style={{ marginBottom: 0 }}>
                {csvMessage}
              </p>
            )}
          </div>

          <div className="card">
            {loading && <p className="muted">불러오는 중...</p>}
            {!loading && recruitmentApplicants.length === 0 && <p className="muted">기록된 지원서가 없어요.</p>}
            {!loading && recruitmentApplicants.length > 0 && (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>이름</th>
                    <th>연락처</th>
                    <th>메모</th>
                    <th>상태</th>
                    <th>기록일</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {recruitmentApplicants.map((a) => (
                    <tr key={a.id}>
                      <td>{a.name}</td>
                      <td>{a.contact}</td>
                      <td>{a.note}</td>
                      <td>
                        <select
                          value={a.status}
                          onChange={(e) => handleRecruitmentStatusChange(a, e.target.value as RecruitmentStatus)}
                        >
                          {(Object.keys(RECRUITMENT_STATUS_LABELS) as RecruitmentStatus[]).map((s) => (
                            <option key={s} value={s}>
                              {RECRUITMENT_STATUS_LABELS[s]}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>{new Date(a.createdAt).toLocaleDateString("ko-KR")}</td>
                      <td>
                        <button
                          type="button"
                          className="ghost"
                          style={{ padding: "4px 8px", fontSize: 12 }}
                          onClick={() => handleDeleteRecruitmentApplicant(a.id)}
                        >
                          삭제
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {tab === "participants" && (
        <div className="stack" style={{ gap: 12 }}>
          <div className="admin-summary-grid" aria-label="주간 운영 현황">
            <button type="button" className="admin-summary-card" onClick={() => setParticipantFilter("ALL")}><span>전체 참가자</span><strong>{participantSummary.total}명</strong></button>
            <button type="button" className="admin-summary-card good" onClick={() => setParticipantFilter("ELIGIBLE")}><span>환급 대상</span><strong>{participantSummary.eligible}명</strong></button>
            <button type="button" className="admin-summary-card warn" onClick={() => setParticipantFilter("NEEDS_ATTENTION")}><span>미이행</span><strong>{participantSummary.needsAttention}명</strong></button>
            <button type="button" className="admin-summary-card accent" onClick={() => setParticipantFilter("UNPAID")}><span>지급 대기</span><strong>{participantSummary.unpaid}명</strong></button>
            <button type="button" className="admin-summary-card" onClick={() => setParticipantFilter("PAID")}><span>지급 완료</span><strong>{participantSummary.paid}명</strong></button>
            <button type="button" className="admin-summary-card danger" onClick={() => setReviewOpen(true)}><span>사진 검토</span><strong>{reviewActivities.length}건</strong></button>
          </div>
          <div className="card">
            <div className="row-between" style={{ flexWrap: "wrap", gap: 12 }}>
              <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
                <select value={selectedCohort} onChange={(e) => setSelectedCohort(e.target.value)}>
                  <option value="">전체 기수</option>
                  {cohorts.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
                <button type="button" className="ghost" onClick={() => shiftWeek(-7)}>
                  ← 이전 주
                </button>
                <span className="tabular">
                  {weekStartIso} ~ {isoDate(new Date(weekStart.getTime() + 6 * 86400000))}
                </span>
                <button type="button" className="ghost" onClick={() => shiftWeek(7)}>
                  다음 주 →
                </button>
              </div>
              <button type="button" className="ghost" onClick={exportParticipantsCsv} disabled={participants.length === 0}>
                CSV로 내보내기
              </button>
            </div>
            <div className="admin-monitor-tools">
              <input type="search" value={participantSearch} onChange={(e) => setParticipantSearch(e.target.value)} placeholder="닉네임 검색" aria-label="참가자 닉네임 검색" />
              <div className="admin-filter-pills" aria-label="참가자 상태 필터">
                {([["ALL", "전체"], ["NEEDS_ATTENTION", "미이행"], ["ELIGIBLE", "환급 대상"], ["UNPAID", "지급 대기"], ["PAID", "지급 완료"]] as [ParticipantFilter, string][]).map(([value, label]) => (
                  <button key={value} type="button" className={participantFilter === value ? "active" : ""} onClick={() => setParticipantFilter(value)}>{label}</button>
                ))}
              </div>
            </div>
          </div>

          <div className="card">
            {loading && <p className="muted">불러오는 중...</p>}
            {!loading && participants.length === 0 && <p className="muted">이 기수/주에는 참가자가 없어요.</p>}
            {!loading && participants.length > 0 && visibleParticipants.length === 0 && <p className="muted">조건에 맞는 참가자가 없어요.</p>}
            {!loading && visibleParticipants.length > 0 && (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>닉네임</th>
                    <th>기수</th>
                    <th>성공일수</th>
                    <th>요일별 이행</th>
                    <th>환급 대상</th>
                    <th>지급 여부</th>
                    <th>MVP</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {visibleParticipants.map((p) => (
                    <Fragment key={p.userId}>
                      <tr>
                        <td>
                          <button
                            type="button"
                            className="ghost"
                            style={{ padding: 0, fontWeight: 700, textDecoration: "underline" }}
                            onClick={() => openActivities(p)}
                          >
                            {p.nickname}
                          </button>
                        </td>
                        <td>{p.cohort}</td>
                        <td className="tabular">{p.successDays} / 7</td>
                        <td>
                          <div className="admin-day-strip" aria-label={`${p.nickname} 요일별 이행`}>
                            {DAY_LABELS.map((day, index) => {
                              const credit = p.dailyCredits?.[index] ?? 0;
                              const state = credit < 0 ? "future" : credit >= 1 ? "complete" : credit > 0 ? "partial" : "missed";
                              const label = credit < 0 ? "예정" : credit === 1 ? "완료" : credit === 0.5 ? "부분 완료" : "미완료";
                              return <span key={day} className={state} title={`${day}요일: ${label}`}>{day}</span>;
                            })}
                          </div>
                        </td>
                        <td>
                          <span className={`badge ${p.eligible ? "good" : "warn"}`}>
                            {p.eligible ? "대상" : "미대상"}
                          </span>
                        </td>
                        <td>
                          <label className="row" style={{ gap: 6, alignItems: "center" }}>
                            <input type="checkbox" checked={p.paid} onChange={() => handleTogglePaid(p)} />
                            {p.paid ? "지급완료" : "미지급"}
                          </label>
                        </td>
                        <td>
                          <button
                            type="button"
                            className={p.isMvp ? "primary" : "ghost"}
                            style={{ padding: "4px 10px", fontSize: 12 }}
                            onClick={() => handleToggleMvp(p)}
                          >
                            {p.isMvp ? "★ MVP" : "MVP 지정"}
                          </button>
                        </td>
                        <td>
                          <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                            {photoReviewUserIds.has(p.userId) && <span className="role-tag admin-review-tag">사진 검토</span>}
                            {p.role === "ADMIN" && <span className="role-tag">관리자</span>}
                          </div>
                        </td>
                      </tr>
                    </Fragment>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {activityMember && (
        <div className="modal-overlay" onClick={() => setActivityMember(null)}>
          <div
            className="modal-card"
            style={{ width: "min(760px, calc(100vw - 32px))", maxHeight: "85vh", overflowY: "auto" }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="row-between" style={{ marginBottom: 16 }}>
              <div>
                <strong style={{ fontSize: 20 }}>{activityMember.nickname}님의 챌린지 수행내역</strong>
                <div className="muted">{activityMember.cohort} · 날짜별 · 최신순</div>
              </div>
              <button type="button" className="ghost" onClick={() => setActivityMember(null)} aria-label="닫기">×</button>
            </div>

            {activitiesLoading && <p className="muted">수행 내역을 불러오는 중...</p>}
            {activitiesError && <div className="error-banner">{activitiesError}</div>}
            {!activitiesLoading && !activitiesError && activities.length === 0 && (
              <p className="muted">아직 저장된 수행 내역이 없어요.</p>
            )}
            <div className="stack" style={{ gap: 10 }}>
              {activitiesByDate.map(([date, dayActivities]) => {
                const open = openActivityDates.includes(date);
                const dayScore = dayActivities.reduce((sum, a) => sum + (a.earnedScore ?? 0), 0);
                return (
                  <div key={date} className="admin-day-group">
                    <button
                      type="button"
                      className="admin-day-header"
                      onClick={() =>
                        setOpenActivityDates((prev) =>
                          prev.includes(date) ? prev.filter((d) => d !== date) : [...prev, date]
                        )
                      }
                      aria-expanded={open}
                    >
                      <span className="admin-day-toggle">{open ? "▾" : "▸"}</span>
                      <strong>{formatDayLabel(date)}</strong>
                      <span className="muted">
                        {dayActivities.length}건 · {Math.round(dayScore * 100) / 100}점
                      </span>
                    </button>

                    {open && (
                      <div className="stack" style={{ gap: 12, marginTop: 10 }}>
                        {dayActivities.map((activity) => (
                <div className="card" key={`${activity.activityType}-${activity.id}`} style={{ padding: 16 }}>
                  <div className="row-between" style={{ alignItems: "flex-start", gap: 12 }}>
                    <div>
                      <strong>{activity.name}</strong>
                      <div className="muted">{activity.recordDate} · {activity.activityType === "PERSONAL" ? "개인 미션" : "공통 과제"}</div>
                    </div>
                    {activity.aiVerified != null && (
                      <span className={`badge ${activity.aiVerified ? "good" : "warn"}`}>
                        사진 판정 {activity.aiVerified ? "적합" : "검토 필요"}
                      </span>
                    )}
                    {activity.adminInvalidated && <span className="badge danger">인증 무효</span>}
                  </div>
                  <div className="stack" style={{ gap: 6, marginTop: 10 }}>
                    {activity.actualValue != null && (
                      <span>수행 {activity.actualValue}{activity.unit ?? ""}{activity.targetValue != null ? ` / 목표 ${activity.targetValue}${activity.unit ?? ""}` : ""}</span>
                    )}
                    {activity.earnedScore != null && (
                      <span>획득 {Math.round(activity.earnedScore * 100) / 100}점 · 달성률 {Math.round((activity.achievementRate ?? 0) * 100)}%</span>
                    )}
                    {activity.detail && <span style={{ whiteSpace: "pre-wrap" }}>{activity.detail}</span>}
                    {activity.memo && <span className="muted">메모 · {activity.memo}</span>}
                    {activity.photoUrl && <img src={activity.photoUrl} alt={`${activity.name} 인증 사진`} className="photo-preview" />}
                  </div>
                </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {reviewOpen && (
        <div className="modal-overlay" onClick={() => setReviewOpen(false)}>
          <div className="modal-card admin-review-modal" onClick={(e) => e.stopPropagation()}>
            <div className="row-between" style={{ marginBottom: 16 }}>
              <div>
                <strong style={{ fontSize: 20 }}>사진 검토 필요</strong>
                <div className="muted">Vision API가 부적합(false)으로 판정한 인증 · {reviewActivities.length}건</div>
              </div>
              <button type="button" className="ghost" onClick={() => setReviewOpen(false)} aria-label="닫기">×</button>
            </div>
            {reviewActivities.length === 0 && <p className="muted">현재 검토할 사진이 없어요.</p>}
            <div className="admin-review-grid">
              {reviewActivities.map((activity) => (
                <article className="card" key={`${activity.activityType}-${activity.id}`}>
                  <div className="row-between" style={{ alignItems: "flex-start", gap: 8 }}>
                    <div>
                      <strong>{activity.nickname} · {activity.name}</strong>
                      <div className="muted">{activity.recordDate} · {activity.activityType === "PERSONAL" ? "개인 미션" : "공통 과제"}</div>
                    </div>
                    <span className={`badge ${activity.adminInvalidated ? "danger" : "warn"}`}>
                      {activity.adminInvalidated ? "인증 무효" : "검토 필요"}
                    </span>
                  </div>
                  {activity.photoUrl && <img src={activity.photoUrl} alt={`${activity.nickname} ${activity.name} 인증 사진`} className="photo-preview" />}
                  {activity.detail && <p>{activity.detail}</p>}
                  {activity.memo && <p className="muted">메모 · {activity.memo}</p>}
                  <button
                    type="button"
                    className={activity.adminInvalidated ? "ghost" : "danger"}
                    onClick={() => handleActivityInvalidation(activity)}
                  >
                    {activity.adminInvalidated ? "무효 처리 취소" : "인증 무효 처리"}
                  </button>
                </article>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
