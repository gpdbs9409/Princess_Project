package com.example.princessproject.commontask.service;

import com.example.princessproject.commontask.dto.CommonTaskRequest;
import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import com.example.princessproject.commontask.repository.CommonTaskRecordRepository;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the 3 mandatory "공통 과제" (독서/공부/주간회고) - see the notice in SelectionWizard.
 * These are intentionally NOT part of the weighted UserGoal/UserStat/UserMission tree: every
 * participant does them regardless of which capitals they picked, so folding them into that
 * tree would mean either force-attaching a goal/stat the user never chose (corrupting the
 * weight-sum-to-100 rule) or leaving them out entirely for anyone who didn't pick 지식 - which
 * is exactly the bug this exists to fix.
 */
@Service
public class CommonTaskService {

    private static final int MAX_PAGE_NUMBER = 100_000;
    private static final int MAX_PAGES_PER_DAY = 2_000;
    private static final BigDecimal MAX_STUDY_AMOUNT = BigDecimal.valueOf(100_000);
    private static final int MAX_RETRO_TEXT_LENGTH = 5_000;

    private final CommonTaskRecordRepository commonTaskRecordRepository;
    private final UserRepository userRepository;
    private final UserProjectService userProjectService;

    public CommonTaskService(
            CommonTaskRecordRepository commonTaskRecordRepository,
            UserRepository userRepository,
            UserProjectService userProjectService
    ) {
        this.commonTaskRecordRepository = commonTaskRecordRepository;
        this.userRepository = userRepository;
        this.userProjectService = userProjectService;
    }

    @Transactional
    public CommonTaskRecord save(Long userId, CommonTaskRequest request) {
        LocalDate recordDate = normalizeDate(request.taskType(), request.date());
        validate(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = userProjectService.getOrCreateActive(userId);

        CommonTaskRecord record = commonTaskRecordRepository
                .findByUserIdAndTaskTypeAndRecordDate(userId, request.taskType(), recordDate)
                .orElseGet(CommonTaskRecord::new);
        record.setUser(user);
        record.setProject(project);
        record.setTaskType(request.taskType());
        record.setRecordDate(recordDate);
        record.setStartPage(request.startPage());
        record.setEndPage(request.endPage());
        record.setStudyPlannedAmount(request.studyPlannedAmount());
        record.setStudyCompletedAmount(request.studyCompletedAmount());
        record.setRetroDailyLife(request.retroDailyLife());
        record.setRetroWeekReview(request.retroWeekReview());
        record.setRetroNextWeekPlan(request.retroNextWeekPlan());
        record.setMemo(request.memo());
        return commonTaskRecordRepository.save(record);
    }

    @Transactional
    public List<CommonTaskRecord> getDaily(Long userId, LocalDate date) {
        return commonTaskRecordRepository.findByUserIdAndRecordDateAndTaskTypeIn(
                userId, date, List.of(CommonTaskType.READING, CommonTaskType.STUDY));
    }

    @Transactional
    public CommonTaskRecord getWeekly(Long userId, LocalDate anyDayInWeek) {
        LocalDate weekStart = normalizeDate(CommonTaskType.WEEKLY_RETROSPECTIVE, anyDayInWeek);
        return commonTaskRecordRepository
                .findByUserIdAndTaskTypeAndRecordDate(userId, CommonTaskType.WEEKLY_RETROSPECTIVE, weekStart)
                .orElse(null);
    }

    /** WEEKLY_RETROSPECTIVE is always keyed by that week's Monday, no matter which day is passed in. */
    private LocalDate normalizeDate(CommonTaskType taskType, LocalDate date) {
        if (taskType == CommonTaskType.WEEKLY_RETROSPECTIVE) {
            return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return date;
    }

    private void validate(CommonTaskRequest request) {
        switch (request.taskType()) {
            case READING -> validateReading(request);
            case STUDY -> validateStudy(request);
            case WEEKLY_RETROSPECTIVE -> validateRetrospective(request);
        }
    }

    private void validateReading(CommonTaskRequest request) {
        Integer startPage = request.startPage();
        Integer endPage = request.endPage();
        if (startPage == null || endPage == null) {
            throw new CommonTaskValidationException("READING_PAGES_REQUIRED", "Start and end page are required");
        }
        if (startPage < 0 || startPage > MAX_PAGE_NUMBER || endPage < 0 || endPage > MAX_PAGE_NUMBER) {
            throw new CommonTaskValidationException(
                    "READING_PAGE_OUT_OF_RANGE", "Page numbers must be between 0 and " + MAX_PAGE_NUMBER);
        }
        if (endPage < startPage) {
            throw new CommonTaskValidationException(
                    "READING_END_BEFORE_START", "End page must not be before start page");
        }
        if (endPage - startPage > MAX_PAGES_PER_DAY) {
            throw new CommonTaskValidationException(
                    "READING_RANGE_TOO_LARGE", "A single day's reading range can't exceed " + MAX_PAGES_PER_DAY + " pages");
        }
    }

    private void validateStudy(CommonTaskRequest request) {
        BigDecimal completed = request.studyCompletedAmount();
        if (completed == null) {
            throw new CommonTaskValidationException("STUDY_COMPLETED_REQUIRED", "Today's completed amount is required");
        }
        requireInRange(completed, "STUDY_COMPLETED_OUT_OF_RANGE");
        if (request.studyPlannedAmount() != null) {
            requireInRange(request.studyPlannedAmount(), "STUDY_PLANNED_OUT_OF_RANGE");
        }
    }

    private void requireInRange(BigDecimal value, String errorCode) {
        if (value.signum() < 0 || value.compareTo(MAX_STUDY_AMOUNT) > 0) {
            throw new CommonTaskValidationException(errorCode, "Value must be between 0 and " + MAX_STUDY_AMOUNT);
        }
    }

    private void validateRetrospective(CommonTaskRequest request) {
        boolean hasAnyContent = hasText(request.retroDailyLife())
                || hasText(request.retroWeekReview())
                || hasText(request.retroNextWeekPlan());
        if (!hasAnyContent) {
            throw new CommonTaskValidationException(
                    "RETROSPECTIVE_EMPTY", "At least one of the 3 parts must have content");
        }
        if (tooLong(request.retroDailyLife()) || tooLong(request.retroWeekReview()) || tooLong(request.retroNextWeekPlan())) {
            throw new CommonTaskValidationException(
                    "RETROSPECTIVE_TOO_LONG", "Each part must be " + MAX_RETRO_TEXT_LENGTH + " characters or fewer");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean tooLong(String value) {
        return value != null && value.length() > MAX_RETRO_TEXT_LENGTH;
    }
}
