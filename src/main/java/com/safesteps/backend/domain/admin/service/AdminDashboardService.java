package com.safesteps.backend.domain.admin.service;

import com.safesteps.backend.domain.admin.dto.AdminDashboardDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
public class AdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardService.class);

    private final JdbcTemplate jdbcTemplate;
    private final Path scriptsLogDir;

    public AdminDashboardService(JdbcTemplate jdbcTemplate,
                                 @Value("${backend.scheduler.logs.path:src/scripts/logs}") String scriptsLogDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.scriptsLogDir = Path.of(scriptsLogDir);
    }

    public AdminDashboardDTO getDashboard() {
        AdminDashboardDTO dashboard = new AdminDashboardDTO();
        dashboard.setRouteTypeDistribution(routeTypeDistribution());
        dashboard.setTopOrigins(topZones("origin"));
        dashboard.setTopDestinations(topZones("destination"));
        dashboard.setActiveUsers(activeUsers());
        dashboard.setIncidentFunnel(incidentFunnel());
        dashboard.setTopReporters(topReporters());
        dashboard.setPipelineStatus(pipelineStatus());
        dashboard.setLatency(latency());
        dashboard.setErrorRate(errorRate());
        return dashboard;
    }

    private List<AdminDashboardDTO.RouteTypeStat> routeTypeDistribution() {
        List<AdminDashboardDTO.RouteTypeStat> stats = safeList(() -> jdbcTemplate.query("""
                SELECT route_type, COUNT(*) AS total
                FROM admin_route_events
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
                GROUP BY route_type
                ORDER BY total DESC
                """, (rs, rowNum) -> new AdminDashboardDTO.RouteTypeStat(
                rs.getString("route_type"),
                routeLabel(rs.getString("route_type")),
                rs.getLong("total")
        )));

        if (stats.isEmpty()) {
            return List.of(
                    new AdminDashboardDTO.RouteTypeStat("SEGURETAT", routeLabel("SEGURETAT"), 0L),
                    new AdminDashboardDTO.RouteTypeStat("CONFORT", routeLabel("CONFORT"), 0L),
                    new AdminDashboardDTO.RouteTypeStat("CLIMA", routeLabel("CLIMA"), 0L),
                    new AdminDashboardDTO.RouteTypeStat("PERSONALITZAT", routeLabel("PERSONALITZAT"), 0L)
            );
        }
        return stats;
    }

    private List<AdminDashboardDTO.ZoneStat> topZones(String prefix) {
        String zoneColumn = prefix + "_zone";
        String latColumn = prefix + "_lat";
        String lonColumn = prefix + "_lon";

        String sql = String.format("""
                SELECT COALESCE(%1$s, 'Sin zona') AS zone,
                       AVG(%2$s) AS lat,
                       AVG(%3$s) AS lon,
                       COUNT(*) AS total
                FROM admin_route_events
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
                GROUP BY COALESCE(%1$s, 'Sin zona')
                ORDER BY total DESC
                LIMIT 8
                """, zoneColumn, latColumn, lonColumn);

        return safeList(() -> jdbcTemplate.query(sql, (rs, rowNum) -> new AdminDashboardDTO.ZoneStat(
                rs.getString("zone"),
                rs.getDouble("lat"),
                rs.getDouble("lon"),
                rs.getLong("total")
        )));
    }

    private AdminDashboardDTO.ActiveUsersStats activeUsers() {
        AdminDashboardDTO.ActiveUsersStats stats = new AdminDashboardDTO.ActiveUsersStats();

        stats.setDau(safeLong(() -> jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT google_id)
                FROM (
                    SELECT google_id, created_at FROM admin_api_request_metrics WHERE google_id IS NOT NULL
                    UNION ALL
                    SELECT google_id, created_at FROM admin_route_events WHERE google_id IS NOT NULL
                    UNION ALL
                    SELECT google_id, created_at FROM incidents WHERE google_id IS NOT NULL
                ) activity
                WHERE created_at >= CURRENT_DATE
                """, Long.class)));

        stats.setMau(safeLong(() -> jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT google_id)
                FROM (
                    SELECT google_id, created_at FROM admin_api_request_metrics WHERE google_id IS NOT NULL
                    UNION ALL
                    SELECT google_id, created_at FROM admin_route_events WHERE google_id IS NOT NULL
                    UNION ALL
                    SELECT google_id, created_at FROM incidents WHERE google_id IS NOT NULL
                ) activity
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
                """, Long.class)));

        stats.setDailyActiveUsers(safeList(() -> jdbcTemplate.query("""
                SELECT CAST(created_at AS DATE) AS day, COUNT(DISTINCT google_id) AS total
                FROM (
                    SELECT google_id, created_at FROM admin_api_request_metrics WHERE google_id IS NOT NULL
                    UNION ALL
                    SELECT google_id, created_at FROM admin_route_events WHERE google_id IS NOT NULL
                    UNION ALL
                    SELECT google_id, created_at FROM incidents WHERE google_id IS NOT NULL
                ) activity
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '14 days'
                GROUP BY CAST(created_at AS DATE)
                ORDER BY day
                """, (rs, rowNum) -> new AdminDashboardDTO.TimeseriesPoint(
                rs.getString("day"),
                rs.getLong("total")
        ))));

        stats.setRegistrations(safeList(() -> jdbcTemplate.query("""
                SELECT CAST(created_at AS DATE) AS day, COUNT(*) AS total
                FROM users
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
                GROUP BY CAST(created_at AS DATE)
                ORDER BY day
                """, (rs, rowNum) -> new AdminDashboardDTO.TimeseriesPoint(
                rs.getString("day"),
                rs.getLong("total")
        ))));

        return stats;
    }

    private AdminDashboardDTO.IncidentFunnelStats incidentFunnel() {
        return safeObject(() -> jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END) AS accepted,
                       SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected,
                       SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending
                FROM incidents
                """, (rs, rowNum) -> {
            AdminDashboardDTO.IncidentFunnelStats stats = new AdminDashboardDTO.IncidentFunnelStats();
            stats.setTotal(rs.getLong("total"));
            stats.setAccepted(rs.getLong("accepted"));
            stats.setRejected(rs.getLong("rejected"));
            stats.setPending(rs.getLong("pending"));
            return stats;
        }), new AdminDashboardDTO.IncidentFunnelStats());
    }

    private List<AdminDashboardDTO.ReporterStat> topReporters() {
        return safeList(() -> jdbcTemplate.query("""
                SELECT i.google_id,
                       COALESCE(u.username, i.google_id) AS username,
                       COUNT(*) AS incidents,
                       COALESCE(MAX(u.points), 0) AS points,
                       COALESCE(MAX(u.level), 0) AS level
                FROM incidents i
                LEFT JOIN users u ON u.google_id = i.google_id
                WHERE i.google_id IS NOT NULL
                GROUP BY i.google_id, COALESCE(u.username, i.google_id)
                ORDER BY incidents DESC
                LIMIT 8
                """, (rs, rowNum) -> new AdminDashboardDTO.ReporterStat(
                rs.getString("google_id"),
                rs.getString("username"),
                rs.getLong("incidents"),
                rs.getLong("points"),
                rs.getLong("level")
        )));
    }

    private AdminDashboardDTO.PipelineStatus pipelineStatus() {
        AdminDashboardDTO.PipelineStatus status = safeObject(() -> jdbcTemplate.queryForObject("""
                SELECT id, status, started_at, finished_at, duration_ms, error_message
                FROM admin_pipeline_runs
                ORDER BY started_at DESC
                LIMIT 1
                """, (rs, rowNum) -> {
            AdminDashboardDTO.PipelineStatus result = new AdminDashboardDTO.PipelineStatus();
            long runId = rs.getLong("id");
            result.setStatus(rs.getString("status"));
            result.setStartedAt(String.valueOf(rs.getTimestamp("started_at")));
            result.setFinishedAt(rs.getTimestamp("finished_at") == null ? null : String.valueOf(rs.getTimestamp("finished_at")));
            result.setDurationMs(rs.getLong("duration_ms"));
            result.setErrorMessage(rs.getString("error_message"));
            result.setScripts(pipelineScripts(runId));
            return result;
        }), new AdminDashboardDTO.PipelineStatus());

        if ("UNKNOWN".equals(status.getStatus()) || status.getScripts().isEmpty()) {
            return pipelineStatusFromLogs();
        }
        return status;
    }

    private List<AdminDashboardDTO.PipelineScriptStat> pipelineScripts(long runId) {
        return safeList(() -> jdbcTemplate.query("""
                SELECT script_name, status, duration_ms, exit_code
                FROM admin_pipeline_script_runs
                WHERE pipeline_run_id = ?
                ORDER BY id
                """, (rs, rowNum) -> new AdminDashboardDTO.PipelineScriptStat(
                rs.getString("script_name"),
                rs.getString("status"),
                rs.getLong("duration_ms"),
                rs.getObject("exit_code", Integer.class)
        ), runId));
    }

    private AdminDashboardDTO.LatencyStats latency() {
        AdminDashboardDTO.LatencyStats stats = safeObject(() -> jdbcTemplate.queryForObject("""
                SELECT COALESCE(AVG(response_ms), 0) AS average_ms,
                       COALESCE(MAX(response_ms), 0) AS max_ms,
                       COUNT(*) AS samples
                FROM admin_route_events
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
                """, (rs, rowNum) -> {
            AdminDashboardDTO.LatencyStats result = new AdminDashboardDTO.LatencyStats();
            result.setAverageMs(rs.getDouble("average_ms"));
            result.setMaxMs(rs.getLong("max_ms"));
            result.setSamples(rs.getLong("samples"));
            return result;
        }), new AdminDashboardDTO.LatencyStats());

        stats.setSeries(safeList(() -> jdbcTemplate.query("""
                SELECT TO_CHAR(DATE_TRUNC('hour', created_at), 'YYYY-MM-DD HH24:00') AS hour,
                       ROUND(AVG(response_ms)) AS average_ms
                FROM admin_route_events
                WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
                GROUP BY DATE_TRUNC('hour', created_at)
                ORDER BY DATE_TRUNC('hour', created_at)
                """, (rs, rowNum) -> new AdminDashboardDTO.TimeseriesPoint(
                rs.getString("hour"),
                rs.getLong("average_ms")
        ))));

        return stats;
    }

    private AdminDashboardDTO.ErrorRateStats errorRate() {
        return safeObject(() -> jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total_requests,
                       SUM(CASE WHEN status_code >= 500 THEN 1 ELSE 0 END) AS server_errors,
                       CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE ROUND((SUM(CASE WHEN status_code >= 500 THEN 1 ELSE 0 END)::numeric * 100.0) / COUNT(*), 2)
                       END AS error_percentage
                FROM admin_api_request_metrics
                WHERE path LIKE '/api/v1/%'
                  AND created_at >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
                """, (rs, rowNum) -> {
            AdminDashboardDTO.ErrorRateStats stats = new AdminDashboardDTO.ErrorRateStats();
            stats.setTotalRequests(rs.getLong("total_requests"));
            stats.setServerErrors(rs.getLong("server_errors"));
            stats.setPercentage(rs.getDouble("error_percentage"));
            return stats;
        }), new AdminDashboardDTO.ErrorRateStats());
    }

    private AdminDashboardDTO.PipelineStatus pipelineStatusFromLogs() {
        AdminDashboardDTO.PipelineStatus status = new AdminDashboardDTO.PipelineStatus();
        if (!Files.isDirectory(scriptsLogDir)) {
            return status;
        }

        List<AdminDashboardDTO.PipelineScriptStat> scripts = safeIoList(() -> {
            try (Stream<Path> files = Files.list(scriptsLogDir)) {
                return files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".log"))
                        .sorted(Comparator.comparing(this::lastModified).reversed())
                        .limit(9)
                        .map(this::scriptStatFromLog)
                        .toList();
            }
        });

        if (scripts.isEmpty()) return status;

        boolean failed = scripts.stream().anyMatch(script -> "FAILED".equals(script.getStatus()));
        status.setStatus(failed ? "FAILED" : "SUCCESS");
        status.setScripts(scripts);
        status.setFinishedAt(formatInstant(latestLogInstant()));
        return status;
    }

    private AdminDashboardDTO.PipelineScriptStat scriptStatFromLog(Path path) {
        String filename = path.getFileName().toString();
        String scriptName = filename.replaceFirst("^\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}_", "")
                .replace(".log", "");
        String status = "SUCCESS";
        try {
            String content = Files.readString(path);
            if (content.contains("[ERROR]") || content.contains("[FALLO]") || content.contains("Traceback")) {
                status = "FAILED";
            }
        } catch (IOException ex) {
            status = "UNKNOWN";
        }
        return new AdminDashboardDTO.PipelineScriptStat(scriptName, status, 0L, null);
    }

    private Instant latestLogInstant() {
        if (!Files.isDirectory(scriptsLogDir)) return Instant.EPOCH;
        try (Stream<Path> files = Files.list(scriptsLogDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .map(this::lastModified)
                    .max(Instant::compareTo)
                    .orElse(Instant.EPOCH);
        } catch (IOException ex) {
            logger.debug("Pipeline log directory unavailable: {}", ex.getMessage());
            return Instant.EPOCH;
        }
    }

    private Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException ex) {
            return Instant.EPOCH;
        }
    }

    private String formatInstant(Instant instant) {
        if (instant == null || Instant.EPOCH.equals(instant)) return null;
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    private String routeLabel(String routeType) {
        return switch (routeType) {
            case "SEGURETAT" -> "Rutas seguras";
            case "CONFORT" -> "Rutas accesibles";
            case "CLIMA" -> "Rutas climaticas";
            case "PERSONALITZAT" -> "Personalizadas";
            default -> routeType;
        };
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (DataAccessException ex) {
            logger.debug("Dashboard query unavailable: {}", ex.getMessage());
            return List.of();
        }
    }

    private <T> List<T> safeIoList(IoSupplier<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (IOException ex) {
            logger.debug("Dashboard IO query unavailable: {}", ex.getMessage());
            return List.of();
        }
    }

    private Long safeLong(Supplier<Long> supplier) {
        try {
            Long value = supplier.get();
            return value == null ? 0L : value;
        } catch (DataAccessException ex) {
            logger.debug("Dashboard scalar query unavailable: {}", ex.getMessage());
            return 0L;
        }
    }

    private <T> T safeObject(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (DataAccessException ex) {
            logger.debug("Dashboard object query unavailable: {}", ex.getMessage());
            return fallback;
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
