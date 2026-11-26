package com.imdbselector.infrastructure.adapter.in.http;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window IP rate limiter for sensitive public endpoints.
 */
public final class RateLimitFilter {
    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> hitsByKey = new ConcurrentHashMap<>();

    public RateLimitFilter(int maxRequests, Duration window, Clock clock) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        this.maxRequests = maxRequests;
        this.window = Objects.requireNonNull(window);
        this.clock = Objects.requireNonNull(clock);
    }

    public void register(Javalin app, String... paths) {
        for (String path : paths) {
            app.before(path, this::enforce);
        }
    }

    private void enforce(Context ctx) {
        if (!"POST".equalsIgnoreCase(ctx.method().name())) {
            return;
        }
        String key = clientIp(ctx) + ":" + ctx.path();
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);

        Deque<Instant> timestamps = hitsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                reject(ctx);
                return;
            }
            timestamps.addLast(now);
        }
    }

    private void reject(Context ctx) {
        String traceId = ctx.attribute("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            ctx.header("X-Trace-Id", traceId);
        }
        ApiDtos.ProblemDetail problem = new ApiDtos.ProblemDetail(
                "https://imdbselector.local/errors/rate-limited",
                "Too Many Requests",
                429,
                "Too many authentication attempts. Please try again later.",
                "RATE_LIMITED",
                traceId
        );
        ctx.header("Retry-After", String.valueOf(window.toSeconds()));
        ctx.status(429).contentType("application/problem+json").json(problem);
        ctx.skipRemainingHandlers();
    }

    private static String clientIp(Context ctx) {
        String realIp = ctx.header("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwarded = ctx.header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return ctx.ip();
    }
}
