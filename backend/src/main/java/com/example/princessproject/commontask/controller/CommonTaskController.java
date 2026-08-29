package com.example.princessproject.commontask.controller;

import com.example.princessproject.commontask.dto.CommonTaskRequest;
import com.example.princessproject.commontask.dto.CommonTaskResponse;
import com.example.princessproject.commontask.service.CommonTaskService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommonTaskController {
    private final CommonTaskService service;

    public CommonTaskController(CommonTaskService service) { this.service = service; }

    @PostMapping("/api/common-tasks")
    public CommonTaskResponse save(Authentication auth, @Valid @RequestBody CommonTaskRequest request) {
        return CommonTaskResponse.from(service.save((Long) auth.getPrincipal(), request));
    }

    @GetMapping("/api/common-tasks/daily")
    public List<CommonTaskResponse> getDaily(Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getDaily((Long) auth.getPrincipal(), date).stream().map(CommonTaskResponse::from).toList();
    }
}
