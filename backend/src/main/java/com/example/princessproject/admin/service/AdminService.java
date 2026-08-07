package com.example.princessproject.admin.service;

import com.example.princessproject.admin.dto.AdjustmentResponse;
import com.example.princessproject.admin.dto.AdminApplicantResponse;
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
import com.example.princessproject.record.service.DailyRecordService;
import com.example.princessproject.record.service.MissionProgress;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public AdminService(
            UserRepository userRepository,
            WeeklyRefundRepository weeklyRefundRepository,
            WeeklyMvpRepository weeklyMvpRepository,
            ScoreAdjustmentRepository scoreAdjustmentRepository,
            RecruitmentApplicantRepository recruitmentApplicantRepository,
            DailyRecordService dailyRecordService
    ) {
        this.userRepository = userRepository;
        this.weeklyRefundRepository = weeklyRefundRepository;
        this.weeklyMvpRepository = weeklyMvpRepository;
        this.scoreAdjustmentRepository = scoreAdjustmentRepository;
        this.recruitmentApplicantRepository = recruitmentApplicantRepository;
        this.dailyRecordService = dailyRecordService;
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
        return userRepository.findDistinctCohorts();
    }

    @Transactional(readOnly = true)
    public List<AdminApplicantResponse> listApplicants() {
        return userRepository.findByCohortIsNullOrderByCreatedAtDesc().stream()
                .map(AdminApplicantResponse::from)
                .toList();
    }

    /**
     * cohort == null -> every tagged participant across all cohorts, sorted by cohort then
     * nickname. Otherwise just that cohort.
     */
    @Transactional
    public List<AdminMemberWeekResponse> listParticipantsForWeek(String cohort, LocalDate weekStart) {
        List<User> members = (cohort == null || cohort.isBlank())
                ? userRepository.findByCohortIsNotNullOrderByCohortAscNicknameAsc()
                : userRepository.findByCohortOrderByNicknameAsc(cohort);

        List<AdminMemberWeekResponse> result = new ArrayList<>();
        for (User user : members) {
            Long mvpUserId = weeklyMvpRepository.findByCohortAndWeekStart(user.getCohort(), weekStart)
                    .map(WeeklyMvp::getUserId)
                    .orElse(null);
            result.add(buildWeekResponse(user, weekStart, mvpUserId));
        }
        return result;
    }

    /** Setting a new MVP for a (cohort, week) replaces whoever held it before - not additive. */
    @Transactional
    public MvpResponse setMvp(Long userId, LocalDate weekStart, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.getCohort() == null) {
            throw new IllegalArgumentException("User has no cohort assigned: " + userId);
        }

        WeeklyMvp mvp = weeklyMvpRepository.findByCohortAndWeekStart(user.getCohort(), weekStart)
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
        user.setCohort(cohort == null || cohort.isBlank() ? null : cohort.trim());
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
        return buildWeekResponse(user, weekStart, mvpUserId);
    }

    private AdminMemberWeekResponse buildWeekResponse(User user, LocalDate weekStart, Long mvpUserId) {
        double successDays = computeSuccessDays(user.getId(), weekStart);
        boolean eligible = successDays >= ELIGIBLE_SUCCESS_DAYS;

        WeeklyRefund refund = weeklyRefundRepository.findByUserIdAndWeekStart(user.getId(), weekStart).orElse(null);
        boolean paid = refund != null && refund.isPaid();

        return new AdminMemberWeekResponse(
                user.getId(),
                user.getNickname(),
                user.getCohort(),
                weekStart,
                weekStart.plusDays(6),
                successDays,
                eligible,
                paid,
                paid ? refund.getAmount() : WEEKLY_REFUND_AMOUNT,
                paid ? refund.getPaidAt() : null,
                user.getId().equals(mvpUserId)
        );
    }

    /**
     * Sums a 0/0.5/1 credit for each of the 7 days in the week: 1.0 if every mission active
     * that day was completed, 0.5 if some (but not all) were, 0 if none were (or the user had
     * no active missions that day). Approximated from the same per-mission completion data
     * DailyRecordService already computes for the daily/weekly report views - there's no
     * separately-tracked "day success" concept in the schema, so this is derived, not stored.
     */
    private double computeSuccessDays(Long userId, LocalDate weekStart) {
        double total = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            MissionProgress progress = dailyRecordService.getMissionProgress(userId, date);
            int completed = progress.completedMissions().size();
            int remaining = progress.remainingMissions().size();
            int activeCount = completed + remaining;
            if (activeCount == 0) {
                continue;
            }
            if (remaining == 0) {
                total += 1.0;
            } else if (completed > 0) {
                total += 0.5;
            }
        }
        return total;
    }
}
