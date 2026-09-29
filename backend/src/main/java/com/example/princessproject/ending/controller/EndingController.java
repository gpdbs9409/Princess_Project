package com.example.princessproject.ending.controller;

import com.example.princessproject.ending.dto.EndingResponse;
import com.example.princessproject.ending.dto.EndingStatusResponse;
import com.example.princessproject.ending.service.EndingException;
import com.example.princessproject.ending.service.EndingPhase;
import com.example.princessproject.ending.service.EndingSchedule;
import com.example.princessproject.ending.service.EndingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * preview=true 는 관리자만 쓸 수 있는 미리보기 - 공개일(10/1) 전에도 dev에서 엔딩 화면/팝업을
 * 실제 데이터로 검증하기 위한 용도다. 일반 참가자가 붙여도 무시된다.
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
    public EndingStatusResponse status(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean preview
    ) {
        boolean adminPreview = preview && isAdmin(authentication);
        EndingPhase phase = schedule.phase();
        if (adminPreview && phase == EndingPhase.BEFORE_REVEAL) phase = EndingPhase.REVEALED;
        return new EndingStatusResponse(
                phase.name(),
                schedule.now().toOffsetDateTime(),
                schedule.revealAt().toOffsetDateTime(),
                schedule.closeAt().toOffsetDateTime(),
                adminPreview);
    }

    @GetMapping("/api/ending")
    public EndingResponse ending(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean preview
    ) {
        boolean adminPreview = preview && isAdmin(authentication);
        if (schedule.phase() == EndingPhase.BEFORE_REVEAL && !adminPreview) {
            throw new EndingException("ENDING_NOT_REVEALED", 403, "Ending is not revealed yet");
        }
        Long userId = (Long) authentication.getPrincipal();
        return endingService.getEnding(userId);
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
