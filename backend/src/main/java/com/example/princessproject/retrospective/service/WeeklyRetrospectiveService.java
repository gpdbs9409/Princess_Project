package com.example.princessproject.retrospective.service;

import com.example.princessproject.commontask.service.CommonTaskValidationException;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.retrospective.dto.WeeklyRetrospectiveRequest;
import com.example.princessproject.retrospective.model.WeeklyRetrospective;
import com.example.princessproject.retrospective.repository.WeeklyRetrospectiveRepository;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyRetrospectiveService {
    private static final int MAX_LENGTH = 5_000;
    private final WeeklyRetrospectiveRepository repository;
    private final UserRepository users;
    private final UserProjectService projects;
    public WeeklyRetrospectiveService(WeeklyRetrospectiveRepository repository, UserRepository users,
                                      UserProjectService projects) {
        this.repository = repository; this.users = users; this.projects = projects;
    }

    @Transactional
    public WeeklyRetrospective save(Long userId, WeeklyRetrospectiveRequest request) {
        validate(request);
        LocalDate weekStart = monday(request.date());
        if (repository.findByUserIdAndWeekStart(userId, weekStart).isPresent()) {
            throw new CommonTaskValidationException(
                    "RETROSPECTIVE_ALREADY_EXISTS", "A retrospective already exists for this week");
        }
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = projects.getOrCreateActive(userId);
        WeeklyRetrospective record = new WeeklyRetrospective();
        record.setUser(user); record.setProject(project); record.setWeekStart(weekStart);
        copy(request, record);
        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public WeeklyRetrospective get(Long userId, LocalDate date) {
        return repository.findByUserIdAndWeekStart(userId, monday(date)).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<WeeklyRetrospective> history(Long userId, LocalDate date) {
        return repository.findByUserIdAndWeekStartBeforeOrderByWeekStartDesc(userId, monday(date));
    }

    @Transactional
    public WeeklyRetrospective update(Long userId, Long id, WeeklyRetrospectiveRequest request) {
        validate(request);
        WeeklyRetrospective record = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Weekly retrospective not found: " + id));
        copy(request, record);
        return repository.save(record);
    }

    private void copy(WeeklyRetrospectiveRequest request, WeeklyRetrospective record) {
        record.setRetroDailyLife(request.retroDailyLife()); record.setRetroWeekReview(request.retroWeekReview());
        record.setRetroNextWeekPlan(request.retroNextWeekPlan());
    }
    private LocalDate monday(LocalDate date) { return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }
    private void validate(WeeklyRetrospectiveRequest r) {
        if (!text(r.retroDailyLife()) && !text(r.retroWeekReview()) && !text(r.retroNextWeekPlan()))
            throw new CommonTaskValidationException("RETROSPECTIVE_EMPTY", "At least one part is required");
        if (longText(r.retroDailyLife()) || longText(r.retroWeekReview()) || longText(r.retroNextWeekPlan()))
            throw new CommonTaskValidationException("RETROSPECTIVE_TOO_LONG", "Each part must be 5000 characters or fewer");
    }
    private boolean text(String value) { return value != null && !value.isBlank(); }
    private boolean longText(String value) { return value != null && value.length() > MAX_LENGTH; }
}
