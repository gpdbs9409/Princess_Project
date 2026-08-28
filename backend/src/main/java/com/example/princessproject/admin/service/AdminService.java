package com.example.princessproject.admin.service;

import com.example.princessproject.admin.dto.AdjustmentResponse;
import com.example.princessproject.admin.dto.AdminApplicantResponse;
import com.example.princessproject.admin.dto.AdminActivityResponse;
import com.example.princessproject.admin.dto.AdminMemberWeekResponse;
import com.example.princessproject.admin.dto.MvpResponse;
import com.example.princessproject.admin.dto.RecruitmentApplicantRequest;
import com.example.princessproject.admin.dto.RecruitmentApplicantResponse;
import com.example.princessproject.admin.model.RecruitmentApplicant;
import com.example.princessproject.admin.model.ScoreAdjustment;
import com.example.princessproject.admin.model.WeeklyMvp;
import com.example.princessproject.admin.model.WeeklyRefund;
import com.example.princessproject.admin.repository.RecruitmentApplicantRepository;
import com.example.princessproject.admin.repository.ScoreAdjustmentRepository;
import com.example.princessproject.admin.repository.WeeklyMvpRepository;
import com.example.princessproject.admin.repository.WeeklyRefundRepository;
import com.example.princessproject.common.CohortNames;
import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.repository.CommonTaskRecordRepository;
import com.example.princessproject.record.model.DailyRecord;
import com.example.princessproject.record.repository.DailyRecordRepository;
import com.example.princessproject.record.service.DailyRecordService;
import com.example.princessproject.record.service.MissionProgress;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    // 주 6일 인증 성공 -> 예치금(100,000원)의 1/4 환급, per the refund policy.
    private static final double ELIGIBLE_SUCCESS_DAYS = 6.0;
    private static final BigDecimal WEEKLY_REFUND_AMOUNT = new BigDecimal("25000");

    private final UserRepository userRepository;
    private final WeeklyRefundRepository weeklyRefundRepository;
    private final WeeklyMvpRepository weeklyMvpRepository;
    private final ScoreAdjustmentRepository scoreAdjustmentRepository;
    private final RecruitmentApplicantRepository recruitmentApplicantRepository;
    private final DailyRecordService dailyRecordService;
    private final DailyRecordRepository dailyRecordRepository;
    private final CommonTaskRecordRepository commonTaskRecordRepository;

    public AdminService(
            UserRepository userRepository,
            WeeklyRefundRepository weeklyRefundRepository,
            WeeklyMvpRepository weeklyMvpRepository,
            ScoreAdjustmentRepository scoreAdjustmentRepository,
            RecruitmentApplicantRepository recruitmentApplicantRepository,
            DailyRecordService dailyRecordService,
            DailyRecordRepository dailyRecordRepository,
            CommonTaskRecordRepository commonTaskRecordRepository
    ) {
        this.userRepository = userRepository;
        this.weeklyRefundRepository = weeklyRefundRepository;
        this.weeklyMvpRepository = weeklyMvpRepository;
        this.scoreAdjustmentRepository = scoreAdjustmentRepository;
        this.recruitmentApplicantRepository = recruitmentApplicantRepository;
        this.dailyRecordService = dailyRecordService;
        this.dailyRecordRepository = dailyRecordRepository;
        this.commonTaskRecordRepository = commonTaskRecordRepository;
    }

    // ---- recruitment applicants (internal-only, decoupled from users) ----

    @Transactional(readOnly = true)
    public List<RecruitmentApplicantResponse> listRecruitmentApplicants() {
        return recruitmentApplicantRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(RecruitmentApplicantResponse::from)
                .toList();
    }

    @Transactional
    public RecruitmentApplicantResponse addRecruitmentApplicant(RecruitmentApplicantRequest request) {
        RecruitmentApplicant applicant = new RecruitmentApplicant(
                request.name(), request.contact(), request.note(), request.status(), request.appliedAt());
        return RecruitmentApplicantResponse.from(recruitmentApplicantRepository.save(applicant));
    }

    /**
     * Bulk import from a spreadsheet. Goes through saveAll so the whole file is one batched
     * flush instead of one INSERT round-trip per row, and one transaction so a malformed
     * sheet can't leave half the applicants imported.
     */
    @Transactional
    public List<RecruitmentApplicantResponse> addRecruitmentApplicants(List<RecruitmentApplicantRequest> requests) {
        List<RecruitmentApplicant> applicants = requests.stream()
                .filter(r -> r.name() != null && !r.name().isBlank())
                .map(r -> new RecruitmentApplicant(
                        r.name(), r.contact(), r.note(), r.status(), r.appliedAt()))
                .toList();
        return recruitmentApplicantRepository.saveAll(applicants).stream()
                .map(RecruitmentApplicantResponse::from)
                .toList();
    }

    @Transactional
    public RecruitmentApplicantResponse updateRecruitmentApplicant(Long id, RecruitmentApplicantRequest request) {
        RecruitmentApplicant applicant = recruitmentApplicantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recruitment applicant not found: " + id));
        applicant.setName(request.name());
        applicant.setContact(request.contact());
        applicant.setNote(request.note());
        if (request.status() != null) {
            applicant.setStatus(request.status());
        }
        applicant.setAppliedAt(request.appliedAt());
        return RecruitmentApplicantResponse.from(recruitmentApplicantRepository.save(applicant));
    }

    @Transactional
    public void deleteRecruitmentApplicant(Long id) {
        recruitmentApplicantRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> listCohorts() {
        return userRepository.findDistinctCohorts().stream()
                .map(CohortNames::canonical)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminApplicantResponse> listApplicants() {
        return userRepository.findByCohortIsNullOrderByCreatedAtDesc().stream()
                .map(AdminApplicantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminActivityResponse> listMemberActivities(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<AdminActivityResponse> result = new ArrayList<>();
        for (DailyRecord record : dailyRecordRepository.findByUserIdOrderByRecordDateDescCreatedAtDesc(userId)) {
            result.add(new AdminActivityResponse(
                    record.getId(), record.getUser().getId(), record.getUser().getNickname(),
                    "PERSONAL", record.getUserMission().displayName(), record.getRecordDate(),
                    record.getInputValue(), record.getTargetValueSnapshot(), record.getUserMission().getUnit(),
                    record.getEarnedScore(), record.getAchievementRate(), null, record.getMemo(), record.getPhotoUrl(),
                    record.getAiVerified(), record.getCreatedAt()));
        }
        for (CommonTaskRecord record : commonTaskRecordRepository.findByUserIdOrderByRecordDateDescCreatedAtDesc(userId)) {
            String detail = switch (record.getTaskType()) {
                case READING -> (record.getBookTitle() == null ? "" : record.getBookTitle() + " · ")
                        + record.getStartPage() + "p~" + record.getEndPage() + "p";
                case STUDY -> "완료 " + record.getStudyCompletedAmount()
                        + (record.getStudyPlannedAmount() == null ? "" : " / 계획 " + record.getStudyPlannedAmount());
                case WEEKLY_RETROSPECTIVE -> String.join(" / ", List.of(
                        nullToEmpty(record.getRetroDailyLife()), nullToEmpty(record.getRetroWeekReview()),
                        nullToEmpty(record.getRetroNextWeekPlan()))).replaceAll("(?: / )+$", "");
            };
            result.add(new AdminActivityResponse(
                    record.getId(), record.getUser().getId(), record.getUser().getNickname(),
                    record.getTaskType().name(), switch (record.getTaskType()) {
                        case READING -> "독서";
                        case STUDY -> "공부";
                        case WEEKLY_RETROSPECTIVE -> "주간 회고";
                    }, record.getRecordDate(), null, null, null, null, null, detail, record.getMemo(),
                    record.getPhotoUrl(), record.getAiVerified(), record.getCreatedAt()));
        }
        result.sort(Comparator.comparing(AdminActivityResponse::recordDate).reversed()
                .thenComparing(AdminActivityResponse::recordedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Transactional(readOnly = true)
    public List<AdminActivityResponse> listActivitiesForReview(String cohort) {
        List<AdminActivityResponse> result = new ArrayList<>();
        for (DailyRecord record : dailyRecordRepository.findByAiVerifiedFalseOrderByRecordDateDescCreatedAtDesc()) {
            if (!matchesCohort(record.getUser(), cohort)) continue;
            result.add(new AdminActivityResponse(
                    record.getId(), record.getUser().getId(), record.getUser().getNickname(),
                    "PERSONAL", record.getUserMission().displayName(), record.getRecordDate(),
                    record.getInputValue(), record.getTargetValueSnapshot(), record.getUserMission().getUnit(),
                    record.getEarnedScore(), record.getAchievementRate(), null, record.getMemo(), record.getPhotoUrl(),
                    false, record.getCreatedAt()));
        }
        for (CommonTaskRecord record : commonTaskRecordRepository.findByAiVerifiedFalseOrderByRecordDateDescCreatedAtDesc()) {
            if (!matchesCohort(record.getUser(), cohort)) continue;
            String detail = switch (record.getTaskType()) {
                case READING -> (record.getBookTitle() == null ? "" : record.getBookTitle() + " · ")
                        + record.getStartPage() + "p~" + record.getEndPage() + "p";
                case STUDY -> "완료 " + record.getStudyCompletedAmount()
                        + (record.getStudyPlannedAmount() == null ? "" : " / 계획 " + record.getStudyPlannedAmount());
                case WEEKLY_RETROSPECTIVE -> "";
            };
            result.add(new AdminActivityResponse(
                    record.getId(), record.getUser().getId(), record.getUser().getNickname(),
                    record.getTaskType().name(), record.getTaskType() == com.example.princessproject.commontask.model.CommonTaskType.READING ? "독서" : "공부",
                    record.getRecordDate(), null, null, null, null, null, detail, record.getMemo(), record.getPhotoUrl(),
                    false, record.getCreatedAt()));
        }
        result.sort(Comparator.comparing(AdminActivityResponse::recordDate).reversed()
                .thenComparing(AdminActivityResponse::recordedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private boolean matchesCohort(User user, String cohort) {
        return cohort == null || cohort.isBlank() || CohortNames.aliases(cohort).contains(user.getCohort());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * cohort == null -> every tagged participant across all cohorts, sorted by cohort then
     * nickname. Otherwise just that cohort.
     *
     * Fetches refunds and MVPs for the whole week ONCE (2 queries total, not 2-per-member) and
     * reuses them via in-memory maps - see buildWeekResponse. The per-member success-day
     * calculation still hits the DB once per member (getWeekDailyProgress), but that's now a
     * single query per member instead of 7.
     */
    @Transactional
    public List<AdminMemberWeekResponse> listParticipantsForWeek(String cohort, LocalDate weekStart) {
        List<User> members = (cohort == null || cohort.isBlank())
                ? userRepository.findByCohortIsNotNullOrderByCohortAscNicknameAsc()
                : userRepository.findByCohortInOrderByNicknameAsc(CohortNames.aliases(cohort));

        Map<Long, WeeklyRefund> refundsByUserId = new HashMap<>();
        for (WeeklyRefund refund : weeklyRefundRepository.findByWeekStart(weekStart)) {
            refundsByUserId.put(refund.getUserId(), refund);
        }
        Map<String, Long> mvpUserIdByCohort = new HashMap<>();
        for (WeeklyMvp mvp : weeklyMvpRepository.findByWeekStart(weekStart)) {
            mvpUserIdByCohort.put(CohortNames.canonical(mvp.getCohort()), mvp.getUserId());
        }

        List<AdminMemberWeekResponse> result = new ArrayList<>();
        for (User user : members) {
            Long mvpUserId = mvpUserIdByCohort.get(CohortNames.canonical(user.getCohort()));
            WeeklyRefund refund = refundsByUserId.get(user.getId());
            result.add(buildWeekResponse(user, weekStart, mvpUserId, refund));
        }
        return result;
    }

    /**
     * Setting a new MVP for a (cohort, week) replaces whoever held it before - not additive.
     *
     * 1인 1회 제한 (주간 MVP 정책 v1.0, 2026-08-20, 시하): 같은 기수 안에서 이미 다른 주차에
     * MVP로 선정된 적이 있는 사람은 다시 후보가 될 수 없다. 같은 주차를 정정하는 경우(운영진이
     * 발표 전에 선택을 바꾸는 경우)는 예외로 허용한다 - 그 주차의 기존 기록을 덮어쓰는 것뿐이라
     * "또 받는" 상황이 아니기 때문.
     *
     * 정책 문서의 "엔딩 등급 +1레벨" 보상 자체는 아직 여기서 자동 적용되지 않는다 - Score 산정식이
     * 확정되기 전이라 등급 시스템도 아직 없다 (정책 문서 7번 항목 참고). 이 메서드는 여전히
     * "누가 그 주 MVP인지"만 기록하고, 실제 보상 지급은 운영진이 수동으로 처리한다.
     */
    @Transactional
    public MvpResponse setMvp(Long userId, LocalDate weekStart, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.getCohort() == null) {
            throw new IllegalArgumentException("User has no cohort assigned: " + userId);
        }

        Optional<WeeklyMvp> existingForThisWeek =
                weeklyMvpRepository.findByCohortAndWeekStart(user.getCohort(), weekStart);

        boolean alreadyWonAnotherWeek = weeklyMvpRepository
                .findByCohortAndUserId(user.getCohort(), user.getId())
                .stream()
                .anyMatch(mvp -> !mvp.getWeekStart().equals(weekStart));
        if (alreadyWonAnotherWeek) {
            throw new AdminValidationException(
                    "MVP_ALREADY_AWARDED",
                    user.getNickname() + "님은 이미 다른 주차에 MVP로 선정된 적이 있어요. "
                            + "1인 1회 제한 규칙에 따라 다시 선정할 수 없습니다.");
        }

        WeeklyMvp mvp = existingForThisWeek
                .orElseGet(() -> new WeeklyMvp(user.getId(), user.getCohort(), weekStart, note));
        mvp.setUserId(user.getId());
        mvp.setNote(note);
        weeklyMvpRepository.save(mvp);
        return MvpResponse.from(mvp, user.getNickname());
    }

    @Transactional
    public void clearMvp(String cohort, LocalDate weekStart) {
        weeklyMvpRepository.findByCohortAndWeekStart(cohort, weekStart).ifPresent(weeklyMvpRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<AdjustmentResponse> listAdjustments(Long userId) {
        return scoreAdjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AdjustmentResponse::from)
                .toList();
    }

    // Append-only by design - see ScoreAdjustment's javadoc. "롤백" is deleteAdjustment,
    // not an edit, so there's always a clean audit trail of what was added and later reverted.
    @Transactional
    public AdjustmentResponse addAdjustment(Long userId, LocalDate weekStart, String statTypeCode, BigDecimal points, String reason) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        ScoreAdjustment adjustment = new ScoreAdjustment(userId, weekStart, statTypeCode, points, reason);
        return AdjustmentResponse.from(scoreAdjustmentRepository.save(adjustment));
    }

    @Transactional
    public void deleteAdjustment(Long adjustmentId) {
        scoreAdjustmentRepository.deleteById(adjustmentId);
    }

    @Transactional
    public User assignCohort(Long userId, String cohort) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        // Blank clears the tag, moving them back to the 지원자(applicant) list.
        user.setCohort(CohortNames.canonical(cohort));
        return userRepository.save(user);
    }

    @Transactional
    public AdminMemberWeekResponse setRefundPaid(Long userId, LocalDate weekStart, boolean paid) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        WeeklyRefund refund = weeklyRefundRepository.findByUserIdAndWeekStart(userId, weekStart)
                .orElseGet(() -> new WeeklyRefund(userId, weekStart));
        refund.setPaid(paid);
        refund.setAmount(WEEKLY_REFUND_AMOUNT);
        refund.setPaidAt(paid ? LocalDateTime.now() : null);
        weeklyRefundRepository.save(refund);

        Long mvpUserId = weeklyMvpRepository.findByCohortAndWeekStart(user.getCohort(), weekStart)
                .map(WeeklyMvp::getUserId)
                .orElse(null);
        return buildWeekResponse(user, weekStart, mvpUserId, refund);
    }

    private AdminMemberWeekResponse buildWeekResponse(User user, LocalDate weekStart, Long mvpUserId, WeeklyRefund refund) {
        List<Double> dailyCredits = computeDailyCredits(user.getId(), weekStart);
        double successDays = dailyCredits.stream().mapToDouble(Double::doubleValue).sum();
        // The terms explicitly allow the weekly refund when six attendance days are met even
        // without the Sunday retrospective. Retrospective is scored separately, not a refund gate.
        boolean eligible = successDays >= ELIGIBLE_SUCCESS_DAYS;
        boolean paid = refund != null && refund.isPaid();

        return new AdminMemberWeekResponse(
                user.getId(),
                user.getNickname(),
                CohortNames.canonical(user.getCohort()),
                weekStart,
                weekStart.plusDays(6),
                successDays,
                dailyCredits,
                eligible,
                paid,
                paid ? refund.getAmount() : WEEKLY_REFUND_AMOUNT,
                paid ? refund.getPaidAt() : null,
                user.getId().equals(mvpUserId),
                user.getRole().name()
        );
    }

    /**
     * Sums a 0/0.5/1 credit for each of the 7 days in the week: 1.0 if every mission active
     * that day was completed, 0.5 if some (but not all) were, 0 if none were (or the user had
     * no active missions that day). Approximated from the same per-mission completion data
     * DailyRecordService already computes for the daily/weekly report views - there's no
     * separately-tracked "day success" concept in the schema, so this is derived, not stored.
     *
     * Uses DailyRecordService#getWeekDailyProgress, which fetches the whole week's records in
     * ONE query instead of one growing-range query per day - this used to be 7 DB round trips
     * per participant (49+ per admin page load for a 7-person cohort), which is what made the
     * admin weekly view noticeably heavy.
     */
    private List<Double> computeDailyCredits(Long userId, LocalDate weekStart) {
        List<Double> credits = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int dayOffset = 0;
        for (MissionProgress progress : dailyRecordService.getWeekDailyRefundProgress(userId, weekStart)) {
            LocalDate date = weekStart.plusDays(dayOffset++);
            if (date.isAfter(today)) {
                credits.add(-1.0); // Future day: visually distinct from a missed day in the admin UI.
                continue;
            }
            int completed = progress.completedMissions().size();
            int remaining = progress.remainingMissions().size();
            int activeCount = completed + remaining;
            if (activeCount == 0) {
                credits.add(0.0);
                continue;
            }
            if (remaining == 0) {
                credits.add(1.0);
            } else if (completed > 0) {
                credits.add(0.5);
            } else {
                credits.add(0.0);
            }
        }
        while (credits.size() < 7) credits.add(-1.0);
        return credits;
    }
}
