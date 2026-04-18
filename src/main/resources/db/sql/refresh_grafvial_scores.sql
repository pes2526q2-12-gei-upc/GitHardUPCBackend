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
ADD COLUMN IF NOT EXISTS score_soroll NUMERIC DEFAULT 0,
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
JOIN cat_fets_penals f ON p.nom_districte = f.nom
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