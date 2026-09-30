package com.example.princessproject.ending.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.princessproject.ending.controller.EndingController;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class EndingReleaseTest {
    private EndingSchedule at(LocalDateTime time) {
        var zone = ZoneId.of("Asia/Seoul");
        return new EndingSchedule(LocalDate.of(2026,9,1), LocalDate.of(2026,9,27),
                LocalDateTime.of(2026,10,1,0,0), LocalDateTime.of(2026,10,21,0,0),
                Clock.fixed(time.atZone(zone).toInstant(), zone));
    }

    @Test
    void blocksBothMemberAndAdminBeforeMidnightKst() {
        var service = mock(EndingService.class);
        var controller = new EndingController(at(LocalDateTime.of(2026,9,30,23,59,59)), service);
        assertThat(controller.status().phase()).isEqualTo("BEFORE_REVEAL");
        assertThat(controller.status().preview()).isFalse();
        for (String role : List.of("ROLE_USER", "ROLE_ADMIN")) {
            var auth = new UsernamePasswordAuthenticationToken(49L, "", List.of(new SimpleGrantedAuthority(role)));
            assertThatThrownBy(() -> controller.ending(auth)).isInstanceOf(EndingException.class);
        }
        verifyNoInteractions(service);
    }

    @Test
    void revealsExactlyAtMidnightKst() {
        var service = mock(EndingService.class);
        var controller = new EndingController(at(LocalDateTime.of(2026,10,1,0,0)), service);
        assertThat(controller.status().phase()).isEqualTo("REVEALED");
        controller.ending(new UsernamePasswordAuthenticationToken(49L, ""));
        verify(service).getEnding(49L);
    }

    @Test
    void productionDoesNotUseDevEarlyReveal() {
        var schedule = new EndingSchedule("2026-09-01", "2026-09-27", "2026-10-01T00:00:00",
                "2026-10-21T00:00:00", "production", "backend-production-e551.up.railway.app",
                "dev", "2026-09-29T00:00:00");
        assertThat(schedule.revealAt().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026,10,1,0,0));
    }
}
