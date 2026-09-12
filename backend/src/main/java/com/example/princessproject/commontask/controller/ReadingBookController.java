package com.example.princessproject.commontask.controller;

import com.example.princessproject.commontask.dto.ReadingBookResponse;
import com.example.princessproject.commontask.dto.RegisterReadingBookRequest;
import com.example.princessproject.commontask.model.ReadingBook;
import com.example.princessproject.commontask.service.ReadingBookService;
import jakarta.validation.Valid;
import java.util.List;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** "지금 읽고 있는 책" 조회/등록 - 독서 인증 화면의 이전 페이지 이어쓰기, 마이페이지의 새 책 등록용. */
@RestController
public class ReadingBookController {

    private final ReadingBookService readingBookService;

    public ReadingBookController(ReadingBookService readingBookService) {
        this.readingBookService = readingBookService;
    }

    @GetMapping("/api/reading-books/active")
    public ReadingBookResponse getActive(Authentication authentication, @RequestParam(required = false) LocalDate beforeDate) {
        Long userId = (Long) authentication.getPrincipal();
        ReadingBook active = readingBookService.getActiveBook(userId);
        Integer lastEndPage = beforeDate == null ? readingBookService.lastRecordedEndPage(userId, active)
                : readingBookService.lastRecordedEndPage(userId, active, beforeDate);
        return ReadingBookResponse.from(active, lastEndPage);
    }

    /** 완독 후 새 책 등록 - 등록과 동시에 새 책이 활성화되고 이전 책은 완독 처리된다. */
    @PostMapping("/api/reading-books")
    public ReadingBookResponse register(Authentication authentication, @Valid @RequestBody RegisterReadingBookRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ReadingBook created = readingBookService.registerNewBook(userId, request.title());
        return ReadingBookResponse.from(created, null);
    }

    @GetMapping("/api/reading-books/history")
    public List<ReadingBookResponse> history(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return readingBookService.history(userId).stream()
                .map(book -> ReadingBookResponse.from(book, null))
                .toList();
    }
}
