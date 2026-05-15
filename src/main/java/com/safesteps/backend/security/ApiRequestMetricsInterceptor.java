package com.safesteps.backend.security;

import com.safesteps.backend.domain.admin.service.AdminMetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ApiRequestMetricsInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "safesteps.metrics.startTime";
    private static final Pattern USER_PATH_PATTERN = Pattern.compile("^/api/v1/users/([^/]+)(?:/.*)?$");

    private final AdminMetricsService adminMetricsService;

    public ApiRequestMetricsInterceptor(AdminMetricsService adminMetricsService) {
        this.adminMetricsService = adminMetricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startTime = request.getAttribute(START_TIME_ATTR);
        if (!(startTime instanceof Long startNanos)) return;

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        int status = ex == null ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        adminMetricsService.recordApiRequest(
                request.getMethod(),
                request.getRequestURI(),
                status,
                durationMs,
                extractGoogleId(request)
        );
    }

    private String extractGoogleId(HttpServletRequest request) {
        String googleId = request.getParameter("googleId");
        if (googleId != null && !googleId.isBlank()) return googleId;

        Matcher matcher = USER_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.matches()) return matcher.group(1);

        return null;
    }
}
