-- SafeSteps WebAdmin dashboard support tables.
-- Run this in PostgreSQL/PostGIS before relying on the dashboard metrics.

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS admin_route_events (
    id BIGSERIAL PRIMARY KEY,
    google_id VARCHAR(100),
    route_type VARCHAR(30) NOT NULL,
    origin_lat DOUBLE PRECISION NOT NULL,
    origin_lon DOUBLE PRECISION NOT NULL,
    destination_lat DOUBLE PRECISION NOT NULL,
    destination_lon DOUBLE PRECISION NOT NULL,
    origin_zone VARCHAR(120),
    destination_zone VARCHAR(120),
    response_ms BIGINT NOT NULL DEFAULT 0,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_route_events_created_at ON admin_route_events (created_at);
CREATE INDEX IF NOT EXISTS idx_admin_route_events_route_type ON admin_route_events (route_type);
CREATE INDEX IF NOT EXISTS idx_admin_route_events_google_id ON admin_route_events (google_id);
CREATE INDEX IF NOT EXISTS idx_admin_route_events_origin_zone ON admin_route_events (origin_zone);
CREATE INDEX IF NOT EXISTS idx_admin_route_events_destination_zone ON admin_route_events (destination_zone);

CREATE TABLE IF NOT EXISTS admin_api_request_metrics (
    id BIGSERIAL PRIMARY KEY,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    status_code INTEGER NOT NULL,
    duration_ms BIGINT NOT NULL,
    google_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_api_metrics_created_at ON admin_api_request_metrics (created_at);
CREATE INDEX IF NOT EXISTS idx_admin_api_metrics_path ON admin_api_request_metrics (path);
CREATE INDEX IF NOT EXISTS idx_admin_api_metrics_status ON admin_api_request_metrics (status_code);
CREATE INDEX IF NOT EXISTS idx_admin_api_metrics_google_id ON admin_api_request_metrics (google_id);

CREATE TABLE IF NOT EXISTS admin_pipeline_runs (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_admin_pipeline_runs_started_at ON admin_pipeline_runs (started_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_pipeline_runs_status ON admin_pipeline_runs (status);

CREATE TABLE IF NOT EXISTS admin_pipeline_script_runs (
    id BIGSERIAL PRIMARY KEY,
    pipeline_run_id BIGINT NOT NULL REFERENCES admin_pipeline_runs(id) ON DELETE CASCADE,
    script_name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    exit_code INTEGER,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_pipeline_script_runs_run_id ON admin_pipeline_script_runs (pipeline_run_id);

-- Required for the "Top Origenes / Destinos" heatmap with real neighborhood names.
-- Import Barcelona neighborhoods into this table or adapt the trigger to your existing barrios table.
CREATE TABLE IF NOT EXISTS admin_zones (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    geom geometry(MultiPolygon, 4326) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_zones_geom ON admin_zones USING GIST (geom);

CREATE OR REPLACE FUNCTION admin_fill_route_event_zones()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.origin_zone IS NULL THEN
        SELECT z.name INTO NEW.origin_zone
        FROM admin_zones z
        WHERE ST_Contains(z.geom, ST_SetSRID(ST_Point(NEW.origin_lon, NEW.origin_lat), 4326))
        LIMIT 1;
    END IF;

    IF NEW.destination_zone IS NULL THEN
        SELECT z.name INTO NEW.destination_zone
        FROM admin_zones z
        WHERE ST_Contains(z.geom, ST_SetSRID(ST_Point(NEW.destination_lon, NEW.destination_lat), 4326))
        LIMIT 1;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_admin_fill_route_event_zones ON admin_route_events;
CREATE TRIGGER trg_admin_fill_route_event_zones
BEFORE INSERT OR UPDATE OF origin_lat, origin_lon, destination_lat, destination_lon
ON admin_route_events
FOR EACH ROW
EXECUTE FUNCTION admin_fill_route_event_zones();

CREATE OR REPLACE VIEW v_admin_route_zone_usage AS
SELECT 'ORIGIN' AS kind, COALESCE(origin_zone, 'Sin zona') AS zone, COUNT(*) AS total
FROM admin_route_events
GROUP BY COALESCE(origin_zone, 'Sin zona')
UNION ALL
SELECT 'DESTINATION' AS kind, COALESCE(destination_zone, 'Sin zona') AS zone, COUNT(*) AS total
FROM admin_route_events
GROUP BY COALESCE(destination_zone, 'Sin zona');

CREATE OR REPLACE VIEW v_admin_mobile_api_daily AS
SELECT CAST(created_at AS DATE) AS day,
       COUNT(*) AS total_requests,
       SUM(CASE WHEN status_code >= 500 THEN 1 ELSE 0 END) AS server_errors,
       ROUND((SUM(CASE WHEN status_code >= 500 THEN 1 ELSE 0 END)::numeric * 100.0) / NULLIF(COUNT(*), 0), 2) AS error_percentage,
       ROUND(AVG(duration_ms)) AS average_latency_ms
FROM admin_api_request_metrics
WHERE path LIKE '/api/v1/%'
GROUP BY CAST(created_at AS DATE);
