package com.example.princessproject.ending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.princessproject.admin.model.WeeklyMvp;
import com.example.princessproject.admin.repository.*;
import com.example.princessproject.catalog.model.GoalType;
import com.example.princessproject.common.model.GoalTypeCode;
import com.example.princessproject.project.model.*;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.record.service.*;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EndingServiceTest {
    @ParameterizedTest
    @CsvSource({
        "49,1기,1396,false,1,2,false",
        "51,1기,2380,true,4,5,false",
        "52,1기,2644,false,5,5,true",
        "57,1기,2688,true,5,5,true",
        "49,1기,1396,true,1,2,false",
        "48,1기,1396,false,1,1,false",
        "49,2기,1396,false,1,1,false",
        "48,1기,1396,true,1,2,false"
    })
    void combinesManualAndWeeklyMvpWithoutChangingScores(
            long id, String cohort, String total, boolean weeklyMvp,
            int base, int stage, boolean teaTime) {
        var schedule = new EndingSchedule(LocalDate.of(2026,9,1), LocalDate.of(2026,9,27),
                LocalDateTime.of(2026,10,1,0,0), LocalDateTime.of(2026,10,21,0,0), Clock.systemUTC());
        var records = mock(DailyRecordService.class);
        var projects = mock(UserProjectService.class);
        var users = mock(UserRepository.class);
        var adjustments = mock(ScoreAdjustmentRepository.class);
        var mvps = mock(WeeklyMvpRepository.class);
        User user = new User("renamed-member", "unused");
        user.setId(id); user.setCohort(cohort);
        when(users.findById(id)).thenReturn(Optional.of(user));
        UserProject project = new UserProject(user, "test");
        project.getGoals().add(new UserGoal(project,
                new GoalType(GoalTypeCode.PHYSICAL, "신체", "", 1), 100, null));
        when(projects.getOrCreateActive(id)).thenReturn(project);
        var zero = new MissionProgress(BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), List.of(), List.of(), Map.of());
        var scored = new MissionProgress(new BigDecimal(total), BigDecimal.ZERO,
                Map.of("physical", new BigDecimal(total)), List.of(), List.of(), Map.of());
        when(records.getPeriodTotalProgress(eq(id), any(), any())).thenReturn(scored, zero, zero, zero);
        when(adjustments.findByUserIdOrderByCreatedAtDesc(id)).thenReturn(List.of());
        when(mvps.findByUserId(id)).thenReturn(weeklyMvp
                ? List.of(new WeeklyMvp(id, cohort, LocalDate.of(2026,8,31), "test")) : List.of());
        var result = new EndingService(schedule, records, projects, users, adjustments, mvps).getEnding(id);
        assertThat(result.baseStage()).isEqualTo(base);
        assertThat(result.stage()).isEqualTo(stage);
        assertThat(result.mvpApplied()).isEqualTo(stage > base);
        assertThat(result.mvpTeaTime()).isEqualTo(teaTime);
        assertThat(result.score100()).isEqualByComparingTo(
                new BigDecimal(total).divide(BigDecimal.valueOf(27), 1, java.math.RoundingMode.HALF_UP));
        assertThat(result.capital()).isEqualTo("PHYSICAL");
    }
}
