package com.example.princessproject.commontask.dto;

import com.example.princessproject.commontask.model.ReadingBook;
import java.time.LocalDate;

public record ReadingBookResponse(
        Long id,
        String title,
        String status,
        LocalDate startedAt,
        LocalDate completedAt,
        /** 이 책에 대해 가장 최근에 기록된 페이지 (없으면 null - 아직 이 책으로는 기록이 없다는 뜻). */
        Integer lastEndPage
) {
    public static ReadingBookResponse from(ReadingBook book, Integer lastEndPage) {
        return new ReadingBookResponse(
                book.getId(), book.getTitle(), book.getStatus().name(),
                book.getStartedAt(), book.getCompletedAt(), lastEndPage);
    }
}
