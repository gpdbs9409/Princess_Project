package com.example.princessproject.ending.controller;

import com.example.princessproject.ending.dto.EndingResponse;
import com.example.princessproject.ending.dto.EndingStatusResponse;
import com.example.princessproject.ending.service.EndingException;
import com.example.princessproject.ending.service.EndingPhase;
import com.example.princessproject.ending.service.EndingSchedule;
import com.example.princessproject.ending.service.EndingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 일정은 관리자에게도 동일하게 적용한다. dev 조기 공개는 EndingSchedule에서만 처리한다.
 */
@RestController
public class EndingController {

    private final EndingSchedule schedule;
    private final EndingService endingService;

    public EndingController(EndingSchedule schedule, EndingService endingService) {
        this.schedule = schedule;
        this.endingService = endingService;
    }

    @GetMapping("/api/ending/status")
    public EndingStatusResponse status() {
        EndingPhase phase = schedule.phase();
        return new EndingStatusResponse(
                phase.name(),
                schedule.now().toOffsetDateTime(),
                schedule.revealAt().toOffsetDateTime(),
                schedule.closeAt().toOffsetDateTime(),
                false);
    }

    @GetMapping("/api/ending")
    public EndingResponse ending(Authentication authentication) {
        if (schedule.phase() == EndingPhase.BEFORE_REVEAL) {
            throw new EndingException("ENDING_NOT_REVEALED", 403, "Ending is not revealed yet");
        }
        Long userId = (Long) authentication.getPrincipal();
        return endingService.getEnding(userId);
    }

}
