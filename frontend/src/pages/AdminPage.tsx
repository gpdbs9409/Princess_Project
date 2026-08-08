import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import {
  addAdminAdjustment,
  addRecruitmentApplicant,
  assignAdminCohort,
  clearAdminMvp,
  deleteAdminAdjustment,
  deleteRecruitmentApplicant,
  getAdminAdjustments,
  getAdminApplicants,
  getAdminCohorts,
  getAdminParticipants,
  getRecruitmentApplicants,
  setAdminMvp,
  setAdminRefundPaid,
  updateRecruitmentApplicant,
} from "../api/endpoints";
import type {
  AdminAdjustmentResponse,
  AdminApplicantResponse,
  AdminMemberWeekResponse,
  RecruitmentApplicantResponse,
  RecruitmentStatus,
} from "../api/types";
import { GOAL_TYPE_CODES, GOAL_TYPE_LABELS } from "../api/types";

type Tab = "participants" | "unassigned" | "recruitment";

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
  const [newCohortInput, setNewCohortInput] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedUserId, setExpandedUserId] = useState<number | null>(null);
  const [adjustments, setAdjustments] = useState<AdminAdjustmentResponse[]>([]);
  const [adjustmentForm, setAdjustmentForm] = useState<{ statTypeCode: string; points: string; reason: string }>({
    statTypeCode: "",
    points: "",
    reason: "",
  });
  const [recruitmentApplicants, setRecruitmentApplicants] = useState<RecruitmentApplicantResponse[]>([]);
  const [recruitmentForm, setRecruitmentForm] = useState(blankRecruitmentForm());

  const weekStartIso = useMemo(() => isoDate(weekStart), [weekStart]);

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
      setParticipants(await getAdminParticipants(selectedCohort || null, weekStartIso));
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

  const handleToggleMvp = async (member: AdminMemberWeekResponse) => {
    if (!member.cohort) return;
    if (member.isMvp) {
      await clearAdminMvp(member.cohort, weekStartIso);
    } else {
      await setAdminMvp(member.userId, weekStartIso);
    }
    await loadParticipants();
  };

  const toggleAdjustmentPanel = async (userId: number) => {
    if (expandedUserId === userId) {
      setExpandedUserId(null);
      return;
    }
    setExpandedUserId(userId);
    setAdjustmentForm({ statTypeCode: "", points: "", reason: "" });
    try {
      setAdjustments(await getAdminAdjustments(userId));
    } catch {
      setAdjustments([]);
    }
  };

  const handleAddAdjustment = async (userId: number) => {
    const points = Number(adjustmentForm.points);
    if (!points || Number.isNaN(points)) return;
    const created = await addAdminAdjustment(userId, {
      weekStart: weekStartIso,
      statTypeCode: adjustmentForm.statTypeCode || undefined,
      points,
      reason: adjustmentForm.reason || undefined,
    });
    setAdjustments((prev) => [created, ...prev]);
    setAdjustmentForm({ statTypeCode: "", points: "", reason: "" });
  };

  const handleRollbackAdjustment = async (adjustmentId: number) => {
    await deleteAdminAdjustment(adjustmentId);
    setAdjustments((prev) => prev.filter((a) => a.id !== adjustmentId));
  };

  const shiftWeek = (deltaDays: number) => {
    const next = new Date(weekStart);
    next.setDate(next.getDate() + deltaDays);
    setWeekStart(mondayOf(next));
  };

  const exportParticipantsCsv = () => {
    const rows: (string | number)[][] = [
      ["닉네임", "기수", "주 시작일", "주 종료일", "성공일수(7일 중)", "환급 대상", "환급 지급여부", "환급액"],
      ...participants.map((p) => [
        p.nickname,
        p.cohort ?? "",
        p.weekStart,
        p.weekEnd,
        p.successDays,
        p.eligible ? "대상" : "미대상",
        p.paid ? "지급완료" : "미지급",
        p.amount,
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
                </tr>
              </thead>
              <tbody>
                {applicants.map((a) => (
                  <tr key={a.userId}>
                    <td>{a.nickname}</td>
                    <td>{new Date(a.appliedAt).toLocaleDateString("ko-KR")}</td>
                    <td>
                      <div className="row" style={{ gap: 6 }}>
                        <input
                          type="text"
                          placeholder="예: 1기"
                          style={{ maxWidth: 100 }}
                          value={newCohortInput[a.userId] ?? ""}
                          onChange={(e) =>
                            setNewCohortInput((prev) => ({ ...prev, [a.userId]: e.target.value }))
                          }
                        />
                        <button
                          type="button"
                          className="ghost"
                          onClick={() => handleAssignCohort(a.userId, newCohortInput[a.userId] ?? "")}
                        >
                          배정
                        </button>
                      </div>
                    </td>
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
              앱 회원(users)과 무관한, 팀 내부 모집 기록용 리스트예요. 지금은 수기 입력만
              가능해요 (엑셀 업로드는 나중에 결정).
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
          </div>

          <div className="card">
            {loading && <p className="muted">불러오는 중...</p>}
            {!loading && participants.length === 0 && <p className="muted">이 기수/주에는 참가자가 없어요.</p>}
            {!loading && participants.length > 0 && (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>닉네임</th>
                    <th>기수</th>
                    <th>성공일수</th>
                    <th>환급 대상</th>
                    <th>환급액</th>
                    <th>지급 여부</th>
                    <th>MVP</th>
                    <th>점수 보정</th>
                  </tr>
                </thead>
                <tbody>
                  {participants.map((p) => (
                    <Fragment key={p.userId}>
                      <tr>
                        <td>{p.nickname}</td>
                        <td>{p.cohort}</td>
                        <td className="tabular">{p.successDays} / 7</td>
                        <td>
                          <span className={`badge ${p.eligible ? "good" : "warn"}`}>
                            {p.eligible ? "대상" : "미대상"}
                          </span>
                        </td>
                        <td className="tabular">{p.amount.toLocaleString("ko-KR")}원</td>
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
                          <button
                            type="button"
                            className="ghost"
                            style={{ padding: "4px 10px", fontSize: 12 }}
                            onClick={() => toggleAdjustmentPanel(p.userId)}
                          >
                            {expandedUserId === p.userId ? "닫기" : "보정 내역"}
                          </button>
                        </td>
                      </tr>
                      {expandedUserId === p.userId && (
                        <tr>
                          <td colSpan={8}>
                            <div className="stack" style={{ gap: 10, padding: "8px 0" }}>
                              <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                                <select
                                  value={adjustmentForm.statTypeCode}
                                  onChange={(e) =>
                                    setAdjustmentForm((f) => ({ ...f, statTypeCode: e.target.value }))
                                  }
                                  style={{ maxWidth: 120 }}
                                >
                                  <option value="">총점 보정</option>
                                  {GOAL_TYPE_CODES.map((code) => (
                                    <option key={code} value={code}>
                                      {GOAL_TYPE_LABELS[code]}
                                    </option>
                                  ))}
                                </select>
                                <input
                                  type="number"
                                  placeholder="점수 (예: 10, -5)"
                                  value={adjustmentForm.points}
                                  onChange={(e) => setAdjustmentForm((f) => ({ ...f, points: e.target.value }))}
                                  style={{ maxWidth: 140 }}
                                />
                                <input
                                  type="text"
                                  placeholder="사유 (예: MVP 보너스, 컴플레인 보정)"
                                  value={adjustmentForm.reason}
                                  onChange={(e) => setAdjustmentForm((f) => ({ ...f, reason: e.target.value }))}
                                  style={{ flex: 1, minWidth: 160 }}
                                />
                                <button type="button" className="primary" onClick={() => handleAddAdjustment(p.userId)}>
                                  보정 추가
                                </button>
                              </div>
                              {adjustments.length === 0 && <p className="muted">보정 내역이 없어요.</p>}
                              {adjustments.map((a) => (
                                <div key={a.id} className="row-between" style={{ fontSize: 13 }}>
                                  <span>
                                    {a.statTypeCode ? GOAL_TYPE_LABELS[a.statTypeCode as keyof typeof GOAL_TYPE_LABELS] : "총점"} ·{" "}
                                    <strong className="tabular">{a.points > 0 ? `+${a.points}` : a.points}</strong>
                                    {a.reason ? ` · ${a.reason}` : ""}
                                  </span>
                                  <button type="button" className="ghost" style={{ padding: "2px 8px", fontSize: 11.5 }} onClick={() => handleRollbackAdjustment(a.id)}>
                                    롤백
                                  </button>
                                </div>
                              ))}
                              <p className="muted" style={{ fontSize: 11.5 }}>
                                * 보정 내역은 기록·추적용이며, 대시보드의 실시간 점수 계산에는 아직 자동 반영되지 않아요.
                              </p>
                            </div>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
