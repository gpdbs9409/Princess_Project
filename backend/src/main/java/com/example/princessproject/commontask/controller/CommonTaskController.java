package com.example.princessproject.commontask.controller;

import com.example.princessproject.commontask.dto.CommonTaskRequest;
import com.example.princessproject.commontask.dto.CommonTaskResponse;
import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.service.CommonTaskService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ownership is derived from the JWT principal, same pattern as DailyRecordController/
 * ProjectController - never a client-supplied userId.
 */
@RestController
public class CommonTaskController {

    private final CommonTaskService commonTaskService;

    public CommonTaskController(CommonTaskService commonTaskService) {
        this.commonTaskService = commonTaskService;
    }

    @PostMapping("/api/common-tasks")
    public CommonTaskResponse save(Authentication authentication, @Valid @RequestBody CommonTaskRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        CommonTaskRecord record = commonTaskService.save(userId, request);
        return CommonTaskResponse.from(record);
    }

    @GetMapping("/api/common-tasks/daily")
    public List<CommonTaskResponse> getDaily(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return commonTaskService.getDaily(userId, date).stream().map(CommonTaskResponse::from).toList();
    }

    @GetMapping("/api/common-tasks/weekly")
    public ResponseEntity<CommonTaskResponse> getWeekly(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        Long userId = (Long) authentication.getPrincipal();
        CommonTaskRecord record = commonTaskService.getWeekly(userId, weekStart);
        if (record == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(CommonTaskResponse.from(record));
    }
}
