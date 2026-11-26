package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.domain.exception.DomainException;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.ForbiddenException;
import com.imdbselector.domain.exception.InvalidCsvException;
import com.imdbselector.domain.exception.NoMatchingMediaException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.exception.UnauthorizedException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Registers request correlation (X-Trace-Id + MDC), request timing logs,
 * and exhaustive RFC 9457 ProblemDetail exception mapping.
 */
public final class HttpObservability {
    private static final Logger log = LoggerFactory.getLogger(HttpObservability.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_ATTR = "traceId";
    private static final String START_NANOS_ATTR = "startNanos";

    private HttpObservability() {
    }

    public static void register(Javalin app) {
        app.before(ctx -> {
            String incoming = ctx.header(TRACE_ID_HEADER);
            String traceId = (incoming != null && !incoming.isBlank())
                    ? incoming.trim()
                    : UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            ctx.attribute(TRACE_ID_ATTR, traceId);
            ctx.attribute(START_NANOS_ATTR, System.nanoTime());
            ctx.header(TRACE_ID_HEADER, traceId);
            MDC.put(TRACE_ID_ATTR, traceId);
        });

        app.after(ctx -> {
            Long start = ctx.attribute(START_NANOS_ATTR);
            long durationMs = start == null ? -1 : (System.nanoTime() - start) / 1_000_000;
            log.info("{} {} -> {} ({} ms) [traceId={}]",
                    ctx.method(), ctx.path(), ctx.status().getCode(), durationMs, traceIdOf(ctx));
            MDC.remove(TRACE_ID_ATTR);
        });

        app.exception(DomainException.class, (e, ctx) -> {
            String traceId = traceIdOf(ctx);
            ApiDtos.ProblemDetail problem = switch (e) {
                case ResourceNotFoundException ex -> problem(404, "Not Found", "NOT_FOUND", ex.getMessage(), traceId);
                case NoMatchingMediaException ex -> problem(404, "No Matching Media", "NO_MATCHING_MEDIA", ex.getMessage(), traceId);
                case DuplicateResourceException ex -> problem(409, "Conflict", "CONFLICT", ex.getMessage(), traceId);
                case InvalidCsvException ex -> problem(400, "Invalid CSV", "INVALID_CSV", ex.getMessage(), traceId);
                case UnauthorizedException ex -> problem(401, "Unauthorized", "UNAUTHORIZED", ex.getMessage(), traceId);
                case ForbiddenException ex -> problem(403, "Forbidden", "FORBIDDEN", ex.getMessage(), traceId);
            };
            ctx.status(problem.status())
                    .contentType("application/problem+json")
                    .json(problem);
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            var problem = problem(400, "Bad Request", "BAD_REQUEST", e.getMessage(), traceIdOf(ctx));
            ctx.status(400)
                    .contentType("application/problem+json")
                    .json(problem);
        });

        app.exception(Exception.class, (e, ctx) -> {
            String traceId = traceIdOf(ctx);
            log.error("Unhandled technical error on {} {} [traceId={}]", ctx.method(), ctx.path(), traceId, e);
            var problem = problem(
                    500,
                    "Internal Server Error",
                    "INTERNAL_ERROR",
                    "An unexpected error occurred.",
                    traceId
            );
            ctx.status(500)
                    .contentType("application/problem+json")
                    .json(problem);
        });
    }

    private static ApiDtos.ProblemDetail problem(int status, String title, String code, String detail, String traceId) {
        return new ApiDtos.ProblemDetail(
                "https://imdbselector.local/errors/" + code.toLowerCase().replace('_', '-'),
                title,
                status,
                detail,
                code,
                traceId
        );
    }

    private static String traceIdOf(Context ctx) {
        String traceId = ctx.attribute(TRACE_ID_ATTR);
        return traceId != null ? traceId : "unknown";
    }
}
