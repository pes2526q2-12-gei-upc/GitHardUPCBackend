---------------------------------------- Creacio de la vista d'admin api ----------------------------------------
-- public.v_admin_mobile_api_daily source

CREATE OR REPLACE VIEW public.v_admin_mobile_api_daily
AS SELECT admin_api_request_metrics.created_at::date AS day,
    count(*) AS total_requests,
    sum(
        CASE
            WHEN admin_api_request_metrics.status_code >= 500 THEN 1
            ELSE 0
        END) AS server_errors,
    round(sum(
        CASE
            WHEN admin_api_request_metrics.status_code >= 500 THEN 1
            ELSE 0
        END)::numeric * 100.0 / NULLIF(count(*), 0)::numeric, 2) AS error_percentage,
    round(avg(admin_api_request_metrics.duration_ms)) AS average_latency_ms
   FROM admin_api_request_metrics
   WHERE admin_api_request_metrics.path::text ~~ '/api/v1/%'::text
   GROUP BY (admin_api_request_metrics.created_at::date);

---------------------------------------- Creacio de la vista d'admin zone ----------------------------------------
CREATE OR REPLACE VIEW public.v_admin_route_zone_usage
AS SELECT 'ORIGIN'::text AS kind,
              COALESCE(admin_route_events.origin_zone, 'Sin zona'::character varying) AS zone,
    count(*) AS total
   FROM admin_route_events
   GROUP BY (COALESCE(admin_route_events.origin_zone, 'Sin zona'::character varying))
   UNION ALL
SELECT 'DESTINATION'::text AS kind,
    COALESCE(admin_route_events.destination_zone, 'Sin zona'::character varying) AS zone,
    count(*) AS total
FROM admin_route_events
GROUP BY (COALESCE(admin_route_events.destination_zone, 'Sin zona'::character varying));

---------------------------------------- Creacio de la vista materialitzada de bcn boundary ----------------------------------------

CREATE MATERIALIZED VIEW public.barcelona_boundary
TABLESPACE pg_default
AS SELECT st_transform(st_concavehull(st_collect(bcn_grafvial_trams.geom), 0.90::double precision), 4326) AS boundary_geom
   FROM bcn_grafvial_trams
   WHERE bcn_grafvial_trams.geom IS NOT NULL
              WITH DATA;

-- View indexes:
CREATE INDEX idx_barcelona_boundary_geom ON public.barcelona_boundary USING gist (boundary_geom);

---------------------------------------- Creacio de la vista materialitzada de v_trams_nodes encarregada de la ruta ----------------------------------------

CREATE MATERIALIZED VIEW public.v_trams_nodes
TABLESPACE pg_default
AS SELECT t."FID" AS fid,
          n_inici."FID" AS source,
          n_final."FID" AS target,
          t."LONGITUD" AS longitud,
          t."NVia_D" AS nom_carrer,
          t.cnt_comissaries,
          t.score_comissaries,
          t.cnt_fonts,
          t.cnt_bancs,
          t.cnt_arbres,
          t.cnt_escales,
          t.cnt_cameres,
          t.cnt_fets_delictius,
          t.cnt_infraccions,
          t.score_soroll,
          t.score_aire,
          t.cnt_refugis_climatics,
          t.cnt_incidents
   FROM bcn_grafvial_trams t
            JOIN bcn_grafvial_nodes n_inici ON t."C_Nus_I" = n_inici."C_Nus"
            JOIN bcn_grafvial_nodes n_final ON t."C_Nus_F" = n_final."C_Nus"
   WHERE t."TVia_D" <> ALL (ARRAY['Viaducte'::text, 'Nus'::text, '-'::text, ' '::text, ''::text])
              WITH DATA;

-- View indexes:
CREATE UNIQUE INDEX idx_vtrams_fid ON public.v_trams_nodes USING btree (fid);
CREATE INDEX idx_vtrams_source ON public.v_trams_nodes USING btree (source);
CREATE INDEX idx_vtrams_target ON public.v_trams_nodes USING btree (target);