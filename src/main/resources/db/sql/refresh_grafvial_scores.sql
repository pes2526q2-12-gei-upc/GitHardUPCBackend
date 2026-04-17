-- 1. Añadir columnas a bcn_grafvial_trams si no existen
ALTER TABLE bcn_grafvial_trams 
ADD COLUMN IF NOT EXISTS cnt_comissaries INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_fonts INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_bancs INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_cameres INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_escales INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cnt_arbres INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS score_comissaries NUMERIC DEFAULT 0;

-- 2. Comisarias -> Grafvial
UPDATE bcn_grafvial_trams t 
SET cnt_comissaries = ( 
    SELECT COUNT(*) 
    FROM bcn_comissaries c 
    JOIN bcn_grafvial_nodes ni ON t."C_Nus_I" = ni."C_Nus" 
    JOIN bcn_grafvial_nodes nf ON t."C_Nus_F" = nf."C_Nus" 
    WHERE ST_DWithin( 
        ST_MakeLine( 
            ST_SetSRID(ST_MakePoint(ni."Coord_X"::numeric, ni."Coord_Y"::numeric), 25831), 
            ST_SetSRID(ST_MakePoint(nf."Coord_X"::numeric, nf."Coord_Y"::numeric), 25831) 
        ), 
        ST_SetSRID(ST_MakePoint(c.geo_epgs_25831_x::numeric, c.geo_epgs_25831_y::numeric), 25831), 
        50 
    ) 
);

-- 3. Fuentes de beber -> Grafvial
UPDATE bcn_grafvial_trams t 
SET cnt_fonts = ( 
    SELECT COUNT(*) 
    FROM bcn_fonts_beure f 
    JOIN bcn_grafvial_nodes ni ON t."C_Nus_I" = ni."C_Nus" 
    JOIN bcn_grafvial_nodes nf ON t."C_Nus_F" = nf."C_Nus" 
    WHERE ST_DWithin( 
        ST_MakeLine( 
            ST_SetSRID(ST_MakePoint(ni."Coord_X"::numeric, ni."Coord_Y"::numeric), 25831), 
            ST_SetSRID(ST_MakePoint(nf."Coord_X"::numeric, nf."Coord_Y"::numeric), 25831) 
        ), 
        ST_SetSRID(ST_MakePoint(f.x_etrs89::numeric, f.y_etrs89::numeric), 25831), 
        12 
    ) 
);

-- 4. Camaras de seguridad -> Grafvial
UPDATE bcn_grafvial_trams t 
SET cnt_cameres = ( 
    SELECT COUNT(*) FROM bcn_cameres_seguretat c 
    WHERE c.longitud IS NOT NULL AND c.latitud IS NOT NULL 
    AND ST_DWithin( 
        t.geom, 
        ST_Transform(ST_SetSRID(ST_MakePoint(c.longitud::numeric, c.latitud::numeric), 4326), 25831), 
        20 
    ) 
);

-- 5. Escaleras mecanicas -> Grafvial
UPDATE bcn_grafvial_trams t 
SET cnt_escales = ( 
    SELECT COUNT(*) 
    FROM bcn_escales_mecaniques e 
    JOIN bcn_grafvial_nodes ni ON t."C_Nus_I" = ni."C_Nus" 
    JOIN bcn_grafvial_nodes nf ON t."C_Nus_F" = nf."C_Nus" 
    WHERE ST_DWithin( 
        ST_MakeLine( 
            ST_SetSRID(ST_MakePoint(ni."Coord_X"::numeric, ni."Coord_Y"::numeric), 25831), 
            ST_SetSRID(ST_MakePoint(nf."Coord_X"::numeric, nf."Coord_Y"::numeric), 25831) 
        ), 
        ST_SetSRID(ST_MakePoint(e.geo_epgs_25831_x::numeric, e.geo_epgs_25831_y::numeric), 25831), 
        12 
    ) 
);

-- 6. Preparar geometria bancos
ALTER TABLE bcn_bancs ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
        
UPDATE bcn_bancs 
SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) 
WHERE longitud IS NOT NULL AND latitud IS NOT NULL;
            
CREATE INDEX IF NOT EXISTS idx_bancs_geom ON bcn_bancs USING GIST(geom);

-- 7. Bancos -> Grafvial
UPDATE bcn_grafvial_trams t 
SET cnt_bancs = ( 
    SELECT COUNT(*) FROM bcn_bancs b 
    WHERE b.geom IS NOT NULL 
    AND ST_DWithin(t.geom, b.geom, 12) 
);

-- 8. Preparar geometria arbolado viario
ALTER TABLE bcn_arbrat_viari ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
ALTER TABLE bcn_arbrat_viari ALTER COLUMN geom TYPE geometry(Point, 25831) USING ST_SetSRID(ST_GeomFromText(geom::text), 25831);
        
UPDATE bcn_arbrat_viari 
SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) 
WHERE longitud IS NOT NULL AND latitud IS NOT NULL;
            
CREATE INDEX IF NOT EXISTS idx_arbrat_viari_geom ON bcn_arbrat_viari USING GIST(geom);

-- 9. Preparar geometria arbolado zona
ALTER TABLE bcn_arbrat_zona ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
ALTER TABLE bcn_arbrat_zona ALTER COLUMN geom TYPE geometry(Point, 25831) USING ST_SetSRID(ST_GeomFromText(geom::text), 25831);
        
UPDATE bcn_arbrat_zona 
SET geom = ST_Transform(ST_SetSRID(ST_MakePoint(longitud::numeric, latitud::numeric), 4326), 25831) 
WHERE longitud IS NOT NULL AND latitud IS NOT NULL;
            
CREATE INDEX IF NOT EXISTS idx_arbrat_zona_geom ON bcn_arbrat_zona USING GIST(geom);

-- 10. Arbolado -> Grafvial
UPDATE bcn_grafvial_trams t 
SET cnt_arbres = ( 
    SELECT COALESCE(SUM(total), 0) FROM ( 
        SELECT COUNT(*) as total FROM bcn_arbrat_viari a 
        WHERE a.geom IS NOT NULL AND ST_DWithin(t.geom, a.geom, 12) 
        UNION ALL 
        SELECT COUNT(*) as total FROM bcn_arbrat_zona a 
        WHERE a.geom IS NOT NULL AND ST_DWithin(t.geom, a.geom, 12) 
    ) as arboles 
);

-- 11. Evitamos RENAME porque no es idempotente, ya se anadió score_comissaries en el paso 1.

-- 12. Preparar geometria comisarias
ALTER TABLE bcn_comissaries ADD COLUMN IF NOT EXISTS geom geometry(Point, 25831);
CREATE INDEX IF NOT EXISTS idx_comissaries_geom ON bcn_comissaries USING GIST(geom);
        
UPDATE bcn_comissaries 
SET geom = ST_SetSRID(ST_MakePoint(geo_epgs_25831_x::numeric, geo_epgs_25831_y::numeric), 25831) 
WHERE geo_epgs_25831_x IS NOT NULL AND geo_epgs_25831_y IS NOT NULL;

-- 13. Update score_comissaries
UPDATE bcn_grafvial_trams t 
SET score_comissaries = ( 
    SELECT COALESCE( 
        MAX( 
            GREATEST(0, 100 * (1 - (ST_Distance(t.geom, c.geom) / 500.0))) 
        ), 
        0 
    ) 
    FROM bcn_comissaries c 
    WHERE ST_DWithin(t.geom, c.geom, 500) 
);
