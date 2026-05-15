package com.safesteps.backend.domain.admin.service;

import com.safesteps.backend.domain.routecalculator.RouteRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Service
public class AdminMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminMetricsService.class);

    private final JdbcTemplate jdbcTemplate;

    public AdminMetricsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordRouteRequest(RouteRequestDTO request, long durationMs, boolean success) {
        if (request == null || request.getFiltre() == null || request.getOrigin() == null || request.getDestination() == null) {
            return;
        }

        executeSafely(() -> jdbcTemplate.update("""
                INSERT INTO admin_route_events
                    (google_id, route_type, origin_lat, origin_lon, destination_lat, destination_lon, response_ms, success)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.getGoogleId(),
                request.getFiltre().name(),
                request.getOrigin().getLat(),
                request.getOrigin().getLon(),
                request.getDestination().getLat(),
                request.getDestination().getLon(),
                durationMs,
                success
        ));
    }

    public void recordApiRequest(String method, String path, int statusCode, long durationMs, String googleId) {
        executeSafely(() -> jdbcTemplate.update("""
                INSERT INTO admin_api_request_metrics
                    (method, path, status_code, duration_ms, google_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                method,
                path,
                statusCode,
                durationMs,
                googleId
        ));
    }

    public Long startPipelineRun() {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO admin_pipeline_runs (status, started_at)
                        VALUES ('RUNNING', CURRENT_TIMESTAMP)
                        """, Statement.RETURN_GENERATED_KEYS);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return key == null ? null : key.longValue();
        } catch (DataAccessException ex) {
            logMetricsUnavailable(ex);
            return null;
        }
    }

    public void finishPipelineRun(Long runId, String status, long durationMs, String errorMessage) {
        if (runId == null) return;
        executeSafely(() -> jdbcTemplate.update("""
                UPDATE admin_pipeline_runs
                SET status = ?, finished_at = CURRENT_TIMESTAMP, duration_ms = ?, error_message = ?
                WHERE id = ?
                """, status, durationMs, errorMessage, runId));
    }

    public void recordPipelineScript(Long runId, String scriptName, String status, long durationMs, Integer exitCode, String errorMessage) {
        if (runId == null) return;
        executeSafely(() -> jdbcTemplate.update("""
                INSERT INTO admin_pipeline_script_runs
                    (pipeline_run_id, script_name, status, duration_ms, exit_code, error_message)
                VALUES (?, ?, ?, ?, ?, ?)
                """, runId, scriptName, status, durationMs, exitCode, errorMessage));
    }

    private void executeSafely(Runnable writeOperation) {
        try {
            writeOperation.run();
        } catch (DataAccessException ex) {
            logMetricsUnavailable(ex);
        }
    }

    private void logMetricsUnavailable(DataAccessException ex) {
        logger.debug("Admin metrics table unavailable. Skipping metric write: {}", ex.getMessage());
    }
}
