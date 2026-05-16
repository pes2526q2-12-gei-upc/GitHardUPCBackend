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
ANALYZE bcn_grafvial_trams;

-- =========================================================================================
-- FASE A: PREPARACIÓN ESPACIAL DE LOS PUNTOS DE INTERÉS
-- =========================================================================================

-- Comisarias
UPDATE bcn_comissaries SET geom = ST_SetSRID(ST_MakePoint(geo_epgs_25831_x::numeric, geo_epgs_25831_y::numeric), 25831) WHERE geo_epgs_25831_x IS NOT NULL AND geo_epgs_25831_y IS NOT NULL;

-- Fuentes de beber
UPDATE bcn_fonts_beure SET geom = ST_SetSRID(ST_MakePoint(x_etrs89::numeric, y_etrs89::numeric), 25831) WHERE x_etrs89 IS NOT NULL AND y_etrs89 IS NOT NULL;

-- Cámaras de seguridad
UPDATE bcn_cameres_seguretat SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;

-- Escaleras mecánicas
UPDATE bcn_escales_mecaniques SET geom = ST_SetSRID(ST_MakePoint(geo_epgs_25831_x::numeric, geo_epgs_25831_y::numeric), 25831) WHERE geo_epgs_25831_x IS NOT NULL AND geo_epgs_25831_y IS NOT NULL;

-- Bancos
UPDATE bcn_bancs SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;

-- Arbolado Viari
UPDATE bcn_arbrat_viari SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;

-- Arbolado Zona
UPDATE bcn_arbrat_zona SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;

-- Contaminación acústica
UPDATE bcn_contaminacio_acustica SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) WHERE longitud IS NOT NULL AND latitud IS NOT NULL;

-- Calidad del aire
UPDATE bcn_qualitat_aire SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831)
WHERE longitud IS NOT NULL AND latitud IS NOT NULL;

-- Refugios climáticos
UPDATE bcn_refugis_climatics SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(geo_epgs_4326_lon::numeric, geo_epgs_4326_lat::numeric), 4326), 25831) WHERE geo_epgs_4326_lon IS NOT NULL AND geo_epgs_4326_lat IS NOT NULL;

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

REFRESH MATERIALIZED VIEW CONCURRENTLY public.v_trams_nodes;
REFRESH MATERIALIZED VIEW public.barcelona_boundary;

