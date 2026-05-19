---------------------------------------- Creacio de la taula d'admin request metrics ----------------------------------------
CREATE TABLE public.admin_api_request_metrics (
                                                  id bigserial NOT NULL,
                                                  "method" varchar(10) NOT NULL,
                                                  "path" varchar(255) NOT NULL,
                                                  status_code int4 NOT NULL,
                                                  duration_ms int8 NOT NULL,
                                                  google_id varchar(100) NULL,
                                                  created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                  CONSTRAINT admin_api_request_metrics_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_admin_api_metrics_created_at ON public.admin_api_request_metrics USING btree (created_at);
CREATE INDEX idx_admin_api_metrics_google_id ON public.admin_api_request_metrics USING btree (google_id);
CREATE INDEX idx_admin_api_metrics_path ON public.admin_api_request_metrics USING btree (path);
CREATE INDEX idx_admin_api_metrics_status ON public.admin_api_request_metrics USING btree (status_code);

---------------------------------------- Creacio de la taula d'admin pipelines ----------------------------------------

CREATE TABLE public.admin_pipeline_runs (
                                            id bigserial NOT NULL,
                                            status varchar(20) NOT NULL,
                                            started_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                            finished_at timestamptz NULL,
                                            duration_ms int8 DEFAULT 0 NOT NULL,
                                            error_message text NULL,
                                            CONSTRAINT admin_pipeline_runs_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_admin_pipeline_runs_started_at ON public.admin_pipeline_runs USING btree (started_at DESC);
CREATE INDEX idx_admin_pipeline_runs_status ON public.admin_pipeline_runs USING btree (status);

---------------------------------------- Creacio de la taula d'admin script pipelines ----------------------------------------

CREATE TABLE public.admin_pipeline_script_runs (
                                                   id bigserial NOT NULL,
                                                   pipeline_run_id int8 NOT NULL,
                                                   script_name varchar(160) NOT NULL,
                                                   status varchar(20) NOT NULL,
                                                   duration_ms int8 DEFAULT 0 NOT NULL,
                                                   exit_code int4 NULL,
                                                   error_message text NULL,
                                                   created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                   CONSTRAINT admin_pipeline_script_runs_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_admin_pipeline_script_runs_run_id ON public.admin_pipeline_script_runs USING btree (pipeline_run_id);


-- public.admin_pipeline_script_runs foreign keys

ALTER TABLE public.admin_pipeline_script_runs ADD CONSTRAINT admin_pipeline_script_runs_pipeline_run_id_fkey FOREIGN KEY (pipeline_run_id) REFERENCES public.admin_pipeline_runs(id) ON DELETE CASCADE;

---------------------------------------- Creacio de la taula d'admin routes i la funcio referenciada pel trigger ----------------------------------------
CREATE TABLE public.admin_route_events (
                                           id bigserial NOT NULL,
                                           google_id varchar(100) NULL,
                                           route_type varchar(30) NOT NULL,
                                           origin_lat float8 NOT NULL,
                                           origin_lon float8 NOT NULL,
                                           destination_lat float8 NOT NULL,
                                           destination_lon float8 NOT NULL,
                                           origin_zone varchar(120) NULL,
                                           destination_zone varchar(120) NULL,
                                           response_ms int8 DEFAULT 0 NOT NULL,
                                           success bool DEFAULT true NOT NULL,
                                           created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                           CONSTRAINT admin_route_events_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_admin_route_events_created_at ON public.admin_route_events USING btree (created_at);
CREATE INDEX idx_admin_route_events_destination_zone ON public.admin_route_events USING btree (destination_zone);
CREATE INDEX idx_admin_route_events_google_id ON public.admin_route_events USING btree (google_id);
CREATE INDEX idx_admin_route_events_origin_zone ON public.admin_route_events USING btree (origin_zone);
CREATE INDEX idx_admin_route_events_route_type ON public.admin_route_events USING btree (route_type);

-- Table Functions

-- DROP FUNCTION public.admin_fill_route_event_zones();

CREATE OR REPLACE FUNCTION public.admin_fill_route_event_zones()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
$function$
;

-- Table Triggers

create trigger trg_admin_fill_route_event_zones before
    insert
    or
update
    of origin_lat,
    origin_lon,
    destination_lat,
    destination_lon on
    public.admin_route_events for each row execute function admin_fill_route_event_zones();

---------------------------------------- Creacio de la taula d'admin zones ----------------------------------------

CREATE TABLE public.admin_zones (
                                    id bigserial NOT NULL,
                                    "name" varchar(120) NOT NULL,
                                    geom public.geometry(multipolygon, 4326) NOT NULL,
                                    CONSTRAINT admin_zones_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_admin_zones_geom ON public.admin_zones USING gist (geom);
