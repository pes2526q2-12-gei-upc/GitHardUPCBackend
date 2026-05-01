package com.safesteps.backend.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AdminDashboardDTO {
    private List<RouteTypeStat> routeTypeDistribution = new ArrayList<>();
    private List<ZoneStat> topOrigins = new ArrayList<>();
    private List<ZoneStat> topDestinations = new ArrayList<>();
    private ActiveUsersStats activeUsers = new ActiveUsersStats();
    private IncidentFunnelStats incidentFunnel = new IncidentFunnelStats();
    private List<ReporterStat> topReporters = new ArrayList<>();
    private PipelineStatus pipelineStatus = new PipelineStatus();
    private LatencyStats latency = new LatencyStats();
    private ErrorRateStats errorRate = new ErrorRateStats();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteTypeStat {
        private String routeType;
        private String label;
        private Long total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneStat {
        private String zone;
        private Double lat;
        private Double lon;
        private Long total;
    }

    @Data
    @NoArgsConstructor
    public static class ActiveUsersStats {
        private Long dau = 0L;
        private Long mau = 0L;
        private List<TimeseriesPoint> dailyActiveUsers = new ArrayList<>();
        private List<TimeseriesPoint> registrations = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class IncidentFunnelStats {
        private Long total = 0L;
        private Long accepted = 0L;
        private Long rejected = 0L;
        private Long pending = 0L;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReporterStat {
        private String googleId;
        private String username;
        private Long incidents;
        private Long points;
        private Long level;
    }

    @Data
    @NoArgsConstructor
    public static class PipelineStatus {
        private String status = "UNKNOWN";
        private String startedAt;
        private String finishedAt;
        private Long durationMs = 0L;
        private String errorMessage;
        private List<PipelineScriptStat> scripts = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineScriptStat {
        private String scriptName;
        private String status;
        private Long durationMs;
        private Integer exitCode;
    }

    @Data
    @NoArgsConstructor
    public static class LatencyStats {
        private Double averageMs = 0.0;
        private Long maxMs = 0L;
        private Long samples = 0L;
        private List<TimeseriesPoint> series = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class ErrorRateStats {
        private Double percentage = 0.0;
        private Long totalRequests = 0L;
        private Long serverErrors = 0L;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeseriesPoint {
        private String label;
        private Long value;
    }
}
