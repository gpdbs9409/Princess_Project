package com.example.princessproject.auth.service;

import com.example.princessproject.ending.service.EndingSchedule;
import com.example.princessproject.ending.service.ServiceClosedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer <token>", and if it's a valid token, sets the authenticated
 * principal to the token's userId (a Long, not a UserDetails - there's no password to check).
 * The token's "role" claim (set at login time) becomes a ROLE_* authority so admin-only
 * endpoints can be gated with .hasRole("ADMIN") in SecurityConfig. Missing/invalid tokens
 * simply leave the request unauthenticated; Spring Security's access rules decide from there
 * whether the endpoint requires authentication.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EndingSchedule endingSchedule;

    public JwtAuthenticationFilter(JwtService jwtService, EndingSchedule endingSchedule) {
        this.jwtService = jwtService;
        this.endingSchedule = endingSchedule;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            Long userId = jwtService.parseUserId(token);
            if (userId != null) {
                String role = jwtService.parseRole(token);
                // 운영 종료(10/21 00:00 KST~) 후에는 이미 로그인돼 있던 세션도 다음 API 요청에서
                // 막는다 (화면설계서 10p). 관리자는 운영 확인을 위해 계속 허용한다.
                if (!"ADMIN".equals(role) && isBlockedWhenClosed(request) && endingSchedule.isClosed()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"" + ServiceClosedException.CODE
                            + "\",\"message\":\"Service closed\"}");
                    return;
                }
                List<SimpleGrantedAuthority> authorities = role != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : List.of();
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isBlockedWhenClosed(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/") && !uri.startsWith("/api/auth/");
    }
}
