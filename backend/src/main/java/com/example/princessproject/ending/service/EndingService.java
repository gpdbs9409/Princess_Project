package com.example.princessproject.ending.service;

import com.example.princessproject.admin.model.ScoreAdjustment;
import com.example.princessproject.admin.model.WeeklyMvp;
import com.example.princessproject.admin.repository.ScoreAdjustmentRepository;
import com.example.princessproject.admin.repository.WeeklyMvpRepository;
import com.example.princessproject.ending.dto.EndingResponse;
import com.example.princessproject.project.model.UserGoal;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.record.service.DailyRecordService;
import com.example.princessproject.record.service.MissionProgress;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공주 엔딩 산정. 산정 기간(1~4주차)의 기록은 이미 지난 날짜라 더 이상 수정되지 않으므로
 * 별도 테이블에 저장하지 않고 조회 시점에 같은 규칙으로 계산한다 - 그래서 공개 기간 중
 * 어드민 보정/무효 처리(이의 제기 반영)가 있으면 자동으로 재계산된 결과가 보인다.
 * (새 테이블을 만들면 ddl-auto=validate 때문에 DB 마이그레이션 선행이 필요해진다.)
 */
@Service
public class EndingService {

    private final EndingSchedule schedule;
    private final DailyRecordService dailyRecordService;
    private final UserProjectService userProjectService;
    private final UserRepository userRepository;
    private final ScoreAdjustmentRepository scoreAdjustmentRepository;
    private final WeeklyMvpRepository weeklyMvpRepository;

    public EndingService(
            EndingSchedule schedule,
            DailyRecordService dailyRecordService,
            UserProjectService userProjectService,
            UserRepository userRepository,
            ScoreAdjustmentRepository scoreAdjustmentRepository,
            WeeklyMvpRepository weeklyMvpRepository
    ) {
        this.schedule = schedule;
        this.dailyRecordService = dailyRecordService;
        this.userProjectService = userProjectService;
        this.userRepository = userRepository;
        this.scoreAdjustmentRepository = scoreAdjustmentRepository;
        this.weeklyMvpRepository = weeklyMvpRepository;
    }

    /** 산정 기간을 월~일 주차로 나눈 구간. 첫 주는 9/1(화)부터, 마지막 주는 9/27(일)까지 자른다. */
    public record WeekRange(int week, LocalDate mondayOfWeek, LocalDate start, LocalDate end) {
    }

    public List<WeekRange> weekRanges() {
        LocalDate periodStart = schedule.periodStart();
        LocalDate periodEnd = schedule.periodEnd();
        List<WeekRange> ranges = new ArrayList<>();
        LocalDate monday = periodStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int week = 1;
        while (!monday.isAfter(periodEnd)) {
            LocalDate start = monday.isBefore(periodStart) ? periodStart : monday;
            LocalDate sunday = monday.plusDays(6);
            LocalDate end = sunday.isAfter(periodEnd) ? periodEnd : sunday;
            ranges.add(new WeekRange(week++, monday, start, end));
            monday = monday.plusWeeks(1);
        }
        return ranges;
    }

    // Not readOnly: getOrCreateActive() may insert a project (see DailyRecordService).
    @Transactional
    public EndingResponse getEnding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<String> selectedCapitals = new ArrayList<>();
        for (UserGoal goal : project.getGoals()) {
            selectedCapitals.add(goal.getGoalType().getCode().name().toLowerCase());
        }
        if (selectedCapitals.isEmpty()) {
            throw new EndingException("ENDING_NOT_AVAILABLE", 404, "No capitals selected");
        }

        List<WeekRange> ranges = weekRanges();
        List<Map<String, BigDecimal>> weeklyStatScores = new ArrayList<>();
        for (WeekRange range : ranges) {
            MissionProgress progress = dailyRecordService.getPeriodTotalProgress(userId, range.start(), range.end());
            Map<String, BigDecimal> scores = new LinkedHashMap<>();
            for (String capital : selectedCapitals) scores.put(capital, BigDecimal.ZERO);
            scores.put("common", BigDecimal.ZERO);
            progress.statScores().forEach((key, value) -> scores.merge(key, value, BigDecimal::add));
            weeklyStatScores.add(scores);
        }

        List<EndingCalculator.Adjustment> adjustments = new ArrayList<>();
        for (ScoreAdjustment adjustment : scoreAdjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            Integer weekIndex = null;
            if (adjustment.getWeekStart() != null) {
                weekIndex = weekIndexOf(ranges, adjustment.getWeekStart());
                // 산정 기간 밖(예: 잔여일 주차)의 주간 보정은 반영하지 않는다.
                if (weekIndex == null) continue;
            }
            adjustments.add(new EndingCalculator.Adjustment(
                    weekIndex, adjustment.getStatTypeCode(), adjustment.getPoints()));
        }

        // 운영자가 확인한 1기 MVP 명단과 주간 MVP 기록을 합집합으로 적용한다.
        // boolean 하나로 전달하므로 양쪽에 등록되어 있어도 승급은 한 번뿐이다.
        boolean mvp = EndingMvpOverrides.appliesTo(
                user.getId(), user.getCohort(), schedule.periodStart(), schedule.periodEnd());
        for (WeeklyMvp weeklyMvp : weeklyMvpRepository.findByUserId(userId)) {
            if (weekIndexOf(ranges, weeklyMvp.getWeekStart()) != null) {
                mvp = true;
                break;
            }
        }

        long periodDays = ChronoUnit.DAYS.between(schedule.periodStart(), schedule.periodEnd()) + 1;
        EndingCalculator.Result result = EndingCalculator.calculate(new EndingCalculator.Input(
                selectedCapitals, weeklyStatScores, adjustments, periodDays, mvp));

        return EndingResponse.of(user.getNickname(), selectedCapitals, ranges, result);
    }

    private static Integer weekIndexOf(List<WeekRange> ranges, LocalDate date) {
        for (int i = 0; i < ranges.size(); i++) {
            WeekRange range = ranges.get(i);
            // 주차 키는 보통 그 주 월요일(weekStart)이지만, 주 안의 어느 날짜든 같은 주차로 본다.
            if (!date.isBefore(range.mondayOfWeek()) && !date.isAfter(range.mondayOfWeek().plusDays(6))) {
                return i;
            }
        }
        return null;
    }
}
