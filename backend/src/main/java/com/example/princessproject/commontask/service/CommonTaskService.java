package com.example.princessproject.commontask.service;

import com.example.princessproject.commontask.dto.CommonTaskRequest;
import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import com.example.princessproject.commontask.repository.CommonTaskRecordRepository;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Daily default tasks only: READING and STUDY. */
@Service
public class CommonTaskService {
    private static final int MAX_PAGE_NUMBER = 100_000;
    private static final int MAX_PAGES_PER_DAY = 2_000;
    private final CommonTaskRecordRepository repository;
    private final UserRepository userRepository;
    private final UserProjectService userProjectService;

    public CommonTaskService(CommonTaskRecordRepository repository, UserRepository userRepository,
                             UserProjectService userProjectService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.userProjectService = userProjectService;
    }

    @Transactional
    public CommonTaskRecord save(Long userId, CommonTaskRequest request) {
        validate(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = userProjectService.getOrCreateActive(userId);
        CommonTaskRecord record = repository.findTopByUserIdAndTaskTypeAndRecordDateOrderByCreatedAtDesc(
                        userId, request.taskType(), request.date()).orElseGet(CommonTaskRecord::new);
        record.setUser(user);
        record.setProject(project);
        record.setTaskType(request.taskType());
        record.setRecordDate(request.date());
        record.setBookTitle(request.bookTitle());
        record.setStartPage(request.startPage());
        record.setEndPage(request.endPage());
        record.setStudyPlannedAmount(request.studyPlannedAmount());
        record.setStudyCompletedAmount(request.studyCompletedAmount());
        record.setStudyYoutubeUrl(request.studyYoutubeUrl());
        record.setStudyTakeaway(request.studyTakeaway());
        record.setPhotoUrl(request.photoUrl());
        record.setAiVerified(request.aiVerified());
        record.setMemo(request.memo());
        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public List<CommonTaskRecord> getDaily(Long userId, LocalDate date) {
        Set<CommonTaskType> seen = new HashSet<>();
        return repository.findByUserIdAndRecordDateAndTaskTypeInOrderByCreatedAtDesc(
                        userId, date, List.of(CommonTaskType.READING, CommonTaskType.STUDY))
                .stream().filter(record -> seen.add(record.getTaskType())).toList();
    }

    private void validate(CommonTaskRequest request) {
        switch (request.taskType()) {
            case READING -> validateReading(request);
            case STUDY -> validateStudy(request);
        }
    }

    private void validateReading(CommonTaskRequest request) {
        Integer start = request.startPage();
        Integer end = request.endPage();
        if (start == null || end == null) throw new CommonTaskValidationException("READING_PAGES_REQUIRED", "Pages required");
        if (start < 0 || start > MAX_PAGE_NUMBER || end < 0 || end > MAX_PAGE_NUMBER)
            throw new CommonTaskValidationException("READING_PAGE_OUT_OF_RANGE", "Page out of range");
        if (end < start) throw new CommonTaskValidationException("READING_END_BEFORE_START", "Invalid page range");
        if (end - start > MAX_PAGES_PER_DAY)
            throw new CommonTaskValidationException("READING_RANGE_TOO_LARGE", "Reading range too large");
        requirePhoto(request, "READING_PHOTO_REQUIRED");
    }

    private void validateStudy(CommonTaskRequest request) {
        String studyTask = request.studyYoutubeUrl();
        if (studyTask == null || studyTask.isBlank())
            throw new CommonTaskValidationException("STUDY_YOUTUBE_URL_REQUIRED", "Study task required");
        if (studyTask.length() > 1000)
            throw new CommonTaskValidationException("STUDY_YOUTUBE_URL_TOO_LONG", "Study task too long");
        if (request.studyTakeaway() == null || request.studyTakeaway().isBlank())
            throw new CommonTaskValidationException("STUDY_TAKEAWAY_REQUIRED", "Takeaway required");
        if (request.studyTakeaway().length() > 1000)
            throw new CommonTaskValidationException("STUDY_TAKEAWAY_TOO_LONG", "Takeaway too long");
    }

    private void requirePhoto(CommonTaskRequest request, String code) {
        if (request.photoUrl() == null || request.photoUrl().isBlank())
            throw new CommonTaskValidationException(code, "A photo is required");
    }
}
