package com.example.princessproject.commontask.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.princessproject.commontask.model.*;
import com.example.princessproject.commontask.repository.*;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadingBookServiceTest {
    @Test void continuesFromPreviousDayAndExcludesTodayAndOtherBooks() {
        var repository = mock(CommonTaskRecordRepository.class);
        var service = new ReadingBookService(mock(ReadingBookRepository.class), repository,
                mock(UserRepository.class), mock(UserProjectService.class));
        var project = new UserProject(); project.setId(1L);
        var book = new ReadingBook(); book.setProject(project); book.setTitle("테스트 책");
        book.setStartedAt(LocalDate.of(2026, 9, 1));
        var today = record(project, "테스트 책", 12, 30);
        var other = record(project, "다른 책", 11, 99);
        var previous = record(project, "테스트 책", 11, 15);
        when(repository.findByUserIdOrderByRecordDateDescCreatedAtDesc(1L)).thenReturn(List.of(today, other, previous));
        assertEquals(15, service.lastRecordedEndPage(1L, book, LocalDate.of(2026, 9, 12)));
        book.setStartedAt(LocalDate.of(2026, 9, 12));
        assertNull(service.lastRecordedEndPage(1L, book, LocalDate.of(2026, 9, 12)));
    }
    private CommonTaskRecord record(UserProject project, String title, int day, int end) {
        var record = new CommonTaskRecord(); record.setProject(project); record.setBookTitle(title);
        record.setTaskType(CommonTaskType.READING); record.setRecordDate(LocalDate.of(2026, 9, day));
        record.setEndPage(end); return record;
    }
}
