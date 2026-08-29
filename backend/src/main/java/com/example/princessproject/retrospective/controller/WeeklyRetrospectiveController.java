package com.example.princessproject.retrospective.controller;

import com.example.princessproject.retrospective.dto.*;
import com.example.princessproject.retrospective.model.WeeklyRetrospective;
import com.example.princessproject.retrospective.service.WeeklyRetrospectiveService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weekly-retrospectives")
public class WeeklyRetrospectiveController {
    private final WeeklyRetrospectiveService service;
    public WeeklyRetrospectiveController(WeeklyRetrospectiveService service) { this.service = service; }
    @PostMapping public WeeklyRetrospectiveResponse save(Authentication auth,
            @Valid @RequestBody WeeklyRetrospectiveRequest request) {
        return WeeklyRetrospectiveResponse.from(service.save((Long) auth.getPrincipal(), request));
    }
    @GetMapping public ResponseEntity<WeeklyRetrospectiveResponse> get(Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        WeeklyRetrospective record = service.get((Long) auth.getPrincipal(), weekStart);
        return record == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(WeeklyRetrospectiveResponse.from(record));
    }
    @GetMapping("/history") public List<WeeklyRetrospectiveResponse> history(Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.history((Long) auth.getPrincipal(), weekStart).stream().map(WeeklyRetrospectiveResponse::from).toList();
    }
    @PutMapping("/{id}") public WeeklyRetrospectiveResponse update(Authentication auth, @PathVariable Long id,
            @Valid @RequestBody WeeklyRetrospectiveRequest request) {
        return WeeklyRetrospectiveResponse.from(service.update((Long) auth.getPrincipal(), id, request));
    }
}
