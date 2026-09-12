package com.example.princessproject.commontask.service;

import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import com.example.princessproject.commontask.model.ReadingBook;
import com.example.princessproject.commontask.model.ReadingBookStatus;
import com.example.princessproject.commontask.repository.CommonTaskRecordRepository;
import com.example.princessproject.commontask.repository.ReadingBookRepository;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "지금 읽고 있는 책" 한 권을 관리한다 (2026-09). 병렬독서는 지원하지 않는다 - 사용자당 항상
 * 최대 1권만 ACTIVE 상태다. 새 책을 등록하면 기존 ACTIVE 책은 자동으로 COMPLETED 처리되고
 * 새 책이 ACTIVE가 된다 (완독 -> 새 책 등록 -> 자동 활성화).
 */
@Service
public class ReadingBookService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ReadingBookRepository readingBookRepository;
    private final CommonTaskRecordRepository commonTaskRecordRepository;
    private final UserRepository userRepository;
    private final UserProjectService userProjectService;

    public ReadingBookService(
            ReadingBookRepository readingBookRepository,
            CommonTaskRecordRepository commonTaskRecordRepository,
            UserRepository userRepository,
            UserProjectService userProjectService
    ) {
        this.readingBookRepository = readingBookRepository;
        this.commonTaskRecordRepository = commonTaskRecordRepository;
        this.userRepository = userRepository;
        this.userProjectService = userProjectService;
    }

    @Transactional
    public ReadingBook getActiveBook(Long userId) {
        return readingBookRepository.findFirstByUserIdAndStatusOrderByIdDesc(userId, ReadingBookStatus.ACTIVE)
                .orElseGet(() -> createFromLegacyHistory(userId));
    }

    /** getActiveBook()과 별도 호출: 지금 활성 책에 대해 마지막으로 기록된 페이지. */
    @Transactional
    public Integer lastRecordedEndPage(Long userId, ReadingBook activeBook) {
        return commonTaskRecordRepository
                .findFirstByUserIdAndTaskTypeAndRecordDateGreaterThanEqualAndAdminInvalidatedFalseOrderByRecordDateDescCreatedAtDesc(
                        userId, CommonTaskType.READING, activeBook.getStartedAt())
                .map(CommonTaskRecord::getEndPage)
                .orElse(null);
    }

    /**
     * 이 기능이 배포되기 전부터 독서 기록이 있던 사용자를 위한 1회성 이관: 가장 최근 기록의 책
     * 제목으로 ACTIVE 책을 하나 만들어준다 (제목을 아직 안 남긴 아주 초기 사용자라면 빈 제목으로
     * 만들고, 마이페이지에서 바로 이름을 지어줄 수 있게 한다).
     */
    private ReadingBook createFromLegacyHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = userProjectService.getOrCreateActive(userId);

        List<CommonTaskRecord> readingHistory = commonTaskRecordRepository
                .findByUserIdOrderByRecordDateDescCreatedAtDesc(userId).stream()
                .filter(record -> record.getTaskType() == CommonTaskType.READING && !record.isAdminInvalidated())
                .toList();

        String title = "";
        LocalDate startedAt = LocalDate.now(SEOUL);
        if (!readingHistory.isEmpty()) {
            CommonTaskRecord latest = readingHistory.get(0);
            title = latest.getBookTitle() == null ? "" : latest.getBookTitle();
            // 이 사용자가 처음 독서를 기록한 날부터 이 "책"이 시작된 것으로 본다 - 그래야 기존
            // 기록들의 페이지가 이어쓰기 기준(lastRecordedEndPage)에 그대로 잡힌다.
            startedAt = readingHistory.get(readingHistory.size() - 1).getRecordDate();
        } else if (project.getCommonReadingBookTitle() != null && !project.getCommonReadingBookTitle().isBlank()) {
            // 아직 독서 기록은 없지만 온보딩(SelectionWizard)에서 읽을 책을 이미 골라뒀다면
            // 그 제목을 그대로 이어받는다 - 새로 만드는 이 기능 때문에 온보딩에서 고른 책 이름이
            // 사라져 보이면 안 된다.
            title = project.getCommonReadingBookTitle().trim();
        }
        return readingBookRepository.save(new ReadingBook(user, project, title, startedAt));
    }

    @Transactional
    public ReadingBook registerNewBook(Long userId, String title) {
        if (title == null || title.isBlank()) {
            throw new CommonTaskValidationException("BOOK_TITLE_REQUIRED", "책 제목을 입력해주세요");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = userProjectService.getOrCreateActive(userId);

        readingBookRepository.findFirstByUserIdAndStatusOrderByIdDesc(userId, ReadingBookStatus.ACTIVE)
                .ifPresent(previous -> {
                    // 병렬독서 없음 - 새 책을 등록하는 순간 이전 책은 완독 처리된다.
                    previous.setStatus(ReadingBookStatus.COMPLETED);
                    previous.setCompletedAt(LocalDate.now(SEOUL));
                    readingBookRepository.save(previous);
                });

        return readingBookRepository.save(new ReadingBook(user, project, title.trim(), LocalDate.now(SEOUL)));
    }

    /** 마이페이지의 "지금까지 읽은 책" 목록 - 최신순. */
    @Transactional
    public List<ReadingBook> history(Long userId) {
        return readingBookRepository.findByUserIdOrderByIdDesc(userId);
    }
}
