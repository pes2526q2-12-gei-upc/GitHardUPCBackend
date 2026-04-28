-- =========================================================================================
-- FASE 0: OPTIMIZACIÓN PREVIA (Forzamos los tipos y el Analizador para evitar Cartesiano)
-- =========================================================================================
CREATE INDEX IF NOT EXISTS idx_grafvial_nodes_cnus ON bcn_grafvial_nodes("C_Nus");
CREATE INDEX IF NOT EXISTS idx_grafvial_trams_ni ON bcn_grafvial_trams("C_Nus_I");
CREATE INDEX IF NOT EXISTS idx_grafvial_trams_nf ON bcn_grafvial_trams("C_Nus_F");

-- (El ANALYZE de estas tablas se realiza directamente desde Java antes de entrar en transaccion)

-- 1. Añadir todas las columnas y la de geometría general
ALTER TABLE bcn_grafvial_trams
ADD COLUMN IF NOT EXISTS cnt_comissaries INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_fonts INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_bancs INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_cameres INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_escales INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_arbres INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS score_comissaries NUMERIC DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_fets_delictius NUMERIC DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_infraccions NUMERIC DEFAULT 0,
ADD COLUMN IF NOT EXISTS score_soroll NUMERIC DEFAULT 0,
ADD COLUMN IF NOT EXISTS score_aire NUMERIC DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_refugis_climatics INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_incidents INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS geom geometry(LineString, 25831);

-- 2. CALCULAR GEOMETRÍAS MAESTRAS DE LAS CALLES
-- Protegido usando subqueries cerradas para evitar una explosión cartesiana (RAM overflow)
-- si OpenData BCN ha introducido nodos duplicados accidentalmente.
UPDATE bcn_grafvial_trams t
SET geom = ST_MakeLine(
    ST_SetSRID(ST_MakePoint(ni."Coord_X"::numeric, ni."Coord_Y"::numeric), 25831),
    ST_SetSRID(ST_MakePoint(nf."Coord_X"::numeric, nf."Coord_Y"::numeric), 25831)
)
FROM 
    (SELECT "C_Nus", MAX("Coord_X") as "Coord_X", MAX("Coord_Y") as "Coord_Y" FROM bcn_grafvial_nodes GROUP BY "C_Nus") ni, 
    (SELECT "C_Nus", MAX("Coord_X") as "Coord_X", MAX("Coord_Y") as "Coord_Y" FROM bcn_grafvial_nodes GROUP BY "C_Nus") nf
WHERE t."C_Nus_I" = ni."C_Nus" AND t."C_Nus_F" = nf."C_Nus"
AND t.geom IS NULL;

-- 3. Crear indice espacial sobre las calles
CREATE INDEX IF NOT EXISTS idx_trams_geom ON bcn_grafvial_trams USING GIST(geom);
ANALYZE bcn_grafvial_trams;

-- =========================================================================================
-- FASE A: PREPARACIÓN ESPACIAL DE LOS PUNTOS DE INTERÉS
-- =========================================================================================

-- Comisarias
ALTER TABLE bcn_comissaries ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_comissaries SET geom = ST_SetSRID(ST_MakePoint(geo_epgs_25831_x::numeric, geo_epgs_25831_y::numeric), 25831) WHERE geo_epgs_25831_x IS NOT NULL AND geo_epgs_25831_y IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_comissaries_geom ON bcn_comissaries USING GIST(geom);

-- Fuentes de beber
ALTER TABLE bcn_fonts_beure ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_fonts_beure SET geom = ST_SetSRID(ST_MakePoint(x_etrs89::numeric, y_etrs89::numeric), 25831) WHERE x_etrs89 IS NOT NULL AND y_etrs89 IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fonts_geom ON bcn_fonts_beure USING GIST(geom);

-- Cámaras de seguridad
ALTER TABLE bcn_cameres_seguretat ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_cameres_seguretat SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_cameres_geom ON bcn_cameres_seguretat USING GIST(geom);

-- Escaleras mecánicas
ALTER TABLE bcn_escales_mecaniques ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_escales_mecaniques SET geom = ST_SetSRID(ST_MakePoint(geo_epgs_25831_x::numeric, geo_epgs_25831_y::numeric), 25831) WHERE geo_epgs_25831_x IS NOT NULL AND geo_epgs_25831_y IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_escales_geom ON bcn_escales_mecaniques USING GIST(geom);

-- Bancos
ALTER TABLE bcn_bancs ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_bancs SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;        
CREATE INDEX IF NOT EXISTS idx_bancs_geom ON bcn_bancs USING GIST(geom);

-- Arbolado Viari
ALTER TABLE bcn_arbrat_viari ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
ALTER TABLE bcn_arbrat_viari ALTER COLUMN geom TYPE geometry(Point, 25831) USING ST_SetSRID(ST_GeomFromText(geom::text), 25831);
UPDATE bcn_arbrat_viari SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;      
CREATE INDEX IF NOT EXISTS idx_arbrat_viari_geom ON bcn_arbrat_viari USING GIST(geom);

-- Arbolado Zona
ALTER TABLE bcn_arbrat_zona ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
ALTER TABLE bcn_arbrat_zona ALTER COLUMN geom TYPE geometry(Point, 25831) USING ST_SetSRID(ST_GeomFromText(geom::text), 25831);
UPDATE bcn_arbrat_zona SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;        
CREATE INDEX IF NOT EXISTS idx_arbrat_zona_geom ON bcn_arbrat_zona USING GIST(geom);

-- Contaminación acústica
ALTER TABLE bcn_contaminacio_acustica ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_contaminacio_acustica SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_soroll_geom ON bcn_contaminacio_acustica USING GIST(geom);

-- Calidad del aire
ALTER TABLE bcn_qualitat_aire ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_qualitat_aire SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831)
WHERE longitud IS NOT NULL AND latitud IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_aire_geom ON bcn_qualitat_aire USING GIST(geom);

-- Refugios climáticos
ALTER TABLE bcn_refugis_climatics ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
UPDATE bcn_refugis_climatics SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(geo_epgs_4326_lon::numeric, geo_epgs_4326_lat::numeric), 4326), 25831) WHERE geo_epgs_4326_lon IS NOT NULL AND geo_epgs_4326_lat IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_refugis_geom ON bcn_refugis_climatics USING GIST(geom);

-- Incidents (tabla dinámica generada por la app, siempre en EPSG:4326)
-- Creamos un índice espacial sobre la geometría transformada a EPSG:25831 para acelerar el cruce espacial
CREATE INDEX IF NOT EXISTS idx_incidents_geom_25831 ON incidents USING GIST(ST_Transform(location, 25831));

-- =========================================================================================
-- FASE B: CRUCE INSTANTÁNEO 
-- =========================================================================================

UPDATE bcn_grafvial_trams t 
SET cnt_comissaries = (SELECT COUNT(*) FROM bcn_comissaries c WHERE c.geom IS NOT NULL AND ST_DWithin(t.geom, c.geom, 50));

UPDATE bcn_grafvial_trams t 
SET cnt_fonts = (SELECT COUNT(*) FROM bcn_fonts_beure f WHERE f.geom IS NOT NULL AND ST_DWithin(t.geom, f.geom, 12));

UPDATE bcn_grafvial_trams t 
SET cnt_cameres = (SELECT COUNT(*) FROM bcn_cameres_seguretat c WHERE c.geom IS NOT NULL AND ST_DWithin(t.geom, c.geom, 20));

UPDATE bcn_grafvial_trams t 
SET cnt_escales = (SELECT COUNT(*) FROM bcn_escales_mecaniques e WHERE e.geom IS NOT NULL AND ST_DWithin(t.geom, e.geom, 12));

UPDATE bcn_grafvial_trams t 
SET cnt_bancs = (SELECT COUNT(*) FROM bcn_bancs b WHERE b.geom IS NOT NULL AND ST_DWithin(t.geom, b.geom, 12));

UPDATE bcn_grafvial_trams t 
SET cnt_arbres = ( 
    SELECT COALESCE(SUM(total), 0) FROM ( 
        SELECT COUNT(*) as total FROM bcn_arbrat_viari a WHERE a.geom IS NOT NULL AND ST_DWithin(t.geom, a.geom, 12) 
        UNION ALL 
        SELECT COUNT(*) as total FROM bcn_arbrat_zona a WHERE a.geom IS NOT NULL AND ST_DWithin(t.geom, a.geom, 12) 
    ) as arboles 
);

UPDATE bcn_grafvial_trams t 
SET score_comissaries = ( 
    SELECT COALESCE(MAX(GREATEST(0, 100 * (1 - (ST_Distance(t.geom, c.geom) / 500.0)))), 0) 
    FROM bcn_comissaries c WHERE c.geom IS NOT NULL AND ST_DWithin(t.geom, c.geom, 500) 
);

UPDATE bcn_grafvial_trams t
SET cnt_fets_delictius = f.total_delictes_vianants
FROM bcn_districtes_poligons p
JOIN cat_fets_penals f ON f.nom_districte = p.nom
WHERE ST_Intersects(t.geom, p.geom);

UPDATE bcn_grafvial_trams t
SET cnt_infraccions = i.nombre_fets_o_infraccions
FROM bcn_districtes_poligons p
JOIN cat_infraccions_joc i ON p.nom = i.nom_districte
WHERE ST_Intersects(t.geom, p.geom);

-- Apliquem una fórmula de mapatge:
-- 50 dB o menys = Score 0
-- 80 dB o més   = Score 100
-- Valor per defecte si no hi ha dades = 55.0 dB (Score ~16.6)
UPDATE bcn_grafvial_trams t
SET score_soroll = (
    SELECT

            LEAST(100.0, GREATEST(0.0, ((COALESCE(AVG(s.nivell_db), 55.0) - 50.0) / 30.0) * 100.0))

    FROM bcn_contaminacio_acustica s
    WHERE s.geom IS NOT NULL
    AND ST_DWithin(t.geom, s.geom, 100)
);
-- Score Aire per a PM2.5 (0-100)
-- Normalització: 25 ug/m3 o més = 100 de score (Molt contaminat per PM2.5)
-- Valor per defecte si no hi ha dades: 10.0 ug/m3 (Score 40)
UPDATE bcn_grafvial_trams t
SET score_aire = (
    SELECT
        -- Multipliquem per 4.0 perquè 25 ug/m3 * 4 = 100 punts de risc
        LEAST(100.0, GREATEST(0.0, (COALESCE(AVG(a.nivell_aire), 10.0) * 4.0)))
    FROM bcn_qualitat_aire a
    WHERE a.geom IS NOT NULL AND ST_DWithin(t.geom, a.geom, 2500)
);

UPDATE bcn_grafvial_trams t
SET cnt_refugis_climatics = (
    SELECT COUNT(*)
    FROM bcn_refugis_climatics r
    WHERE r.geom IS NOT NULL
    AND ST_DWithin(t.geom, r.geom, 50) -- Un radi de 50m sembla adient
);

-- Incidencias aceptadas cerca del tramo (radio: 50m)
-- Contem quantes incidències ACCEPTED hi ha a menys de 50m de cada tram.
-- El resultat es guarda a cnt_incidents i s'usa per penalitzar la ruta.
UPDATE bcn_grafvial_trams t
SET cnt_incidents = (
    SELECT COUNT(*)
    FROM incidents i
    WHERE i.status = 'ACCEPTED'
    AND t.geom IS NOT NULL
    AND ST_DWithin(t.geom, ST_Transform(i.location, 25831), 50)
);
-- =========================================================================================
-- FASE C: RECONSTRUCCIÓN DEL GRAFO DE ENRUTAMIENTO
-- =========================================================================================
-- 1. Eliminar v_trams_nodes independientemente de su tipo actual en BD.
--    Puede existir como TABLE (ejecuciones antiguas), VIEW o MATERIALIZED VIEW.
--    Cubrimos los tres casos para garantizar idempotencia.
DROP TABLE IF EXISTS public.v_trams_nodes CASCADE;
DROP VIEW IF EXISTS public.v_trams_nodes CASCADE;
DROP MATERIALIZED VIEW IF EXISTS public.v_trams_nodes CASCADE;

-- 2. La creamos de nuevo con los datos frescos y TODOS los scores
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
          t.cnt_fets_delictius,
          t.score_soroll,
          t.score_aire,
          t.cnt_refugis_climatics,
          t.cnt_incidents,
          t.cnt_cameres
   FROM bcn_grafvial_trams t
            JOIN bcn_grafvial_nodes n_inici ON t."C_Nus_I" = n_inici."C_Nus"
            JOIN bcn_grafvial_nodes n_final ON t."C_Nus_F" = n_final."C_Nus"
   WHERE t."TVia_D" <> ALL (ARRAY['Viaducte'::text, 'Nus'::text, '-'::text, ' '::text, ''::text])
                   WITH DATA;

-- 3. Recreamos los índices para que el algoritmo JGraphT/pgRouting vuele
CREATE UNIQUE INDEX idx_vtrams_fid ON public.v_trams_nodes USING btree (fid);
CREATE INDEX idx_vtrams_source ON public.v_trams_nodes USING btree (source);
CREATE INDEX idx_vtrams_target ON public.v_trams_nodes USING btree (target);

-- =========================================================================================
-- FASE D: CREACIÓN DEL POLÍGONO DE LÍMITES DE BARCELONA (SSOT)
-- =========================================================================================

-- 1. Vista materializada para el límite de Barcelona usando ST_ConcaveHull
CREATE MATERIALIZED VIEW IF NOT EXISTS barcelona_boundary AS
SELECT ST_Transform(ST_ConcaveHull(ST_Collect(geom), 0.90), 4326) AS boundary_geom
FROM bcn_grafvial_trams
WHERE geom IS NOT NULL
WITH DATA;

-- 2. Índice para acelerar consultas
CREATE INDEX IF NOT EXISTS idx_barcelona_boundary_geom ON barcelona_boundary USING GIST(boundary_geom);
