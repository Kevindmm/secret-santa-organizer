package com.kdmm.secretsanta.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Applies rate limits to the actions that can trigger emails or create data.
 * Uses the first X-Forwarded-For hop when present (Render/proxies) and falls
 * back to the remote address.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration CREATE_WINDOW = Duration.ofHours(1);
    private static final Duration JOIN_WINDOW = Duration.ofMinutes(1);
    private static final Duration ASSIGN_WINDOW = Duration.ofMinutes(1);

    private final RateLimiter rateLimiter;
    private final boolean enabled;
    private final int createGamesPerHour;
    private final int joinPerMinute;
    private final int assignPerMinute;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.create-games-per-hour:5}") int createGamesPerHour,
            @Value("${app.rate-limit.join-per-minute:10}") int joinPerMinute,
            @Value("${app.rate-limit.assign-per-minute:3}") int assignPerMinute) {
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
        this.createGamesPerHour = createGamesPerHour;
        this.joinPerMinute = joinPerMinute;
        this.assignPerMinute = assignPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Limit limit = resolveLimit(path);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + ":" + limit.name();
        if (!rateLimiter.tryAcquire(key, limit.maxRequests(), limit.window())) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(limit.window().toSeconds()));
            response.getWriter().write(
                    "{\"error\":\"Demasiadas solicitudes. Espera un momento antes de volver a intentarlo.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Limit resolveLimit(String path) {
        if (path.matches("/api/games/?")) {
            return new Limit("create-game", createGamesPerHour, CREATE_WINDOW);
        }
        if (path.matches("/api/games/[^/]+/participants/?")) {
            return new Limit("join-game", joinPerMinute, JOIN_WINDOW);
        }
        if (path.matches("/api/games/[^/]+/assign/?")) {
            return new Limit("assign-game", assignPerMinute, ASSIGN_WINDOW);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First entry is the original client; the rest are proxies.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Limit(String name, int maxRequests, Duration window) {}
}
