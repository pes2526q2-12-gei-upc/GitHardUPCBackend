---------------------------------------- Instalacio del sistema de postgis ----------------------------------------
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgrouting;
CREATE EXTENSION IF NOT EXISTS plpgsql;

---------------------------------------- Creacio de la taula d'arbres a la via ----------------------------------------
CREATE TABLE public.bcn_arbrat_viari (
                                         codi text NULL,
                                         x_etrs89 float8 NULL,
                                         y_etrs89 float8 NULL,
                                         latitud float8 NULL,
                                         longitud float8 NULL,
                                         tipus_element text NULL,
                                         espai_verd text NULL,
                                         adreca text NULL,
                                         cat_especie_id int8 NULL,
                                         cat_nom_cientific text NULL,
                                         cat_nom_castella text NULL,
                                         cat_nom_catala text NULL,
                                         categoria_arbrat text NULL,
                                         data_plantacio text NULL,
                                         tipus_aigua text NULL,
                                         tipus_reg text NULL,
                                         geom public.geometry(point, 25831) NULL,
                                         catalogacio text NULL,
                                         codi_barri float8 NULL,
                                         nom_barri text NULL,
                                         codi_districte float8 NULL,
                                         nom_districte text NULL
);
CREATE INDEX idx_arbrat_viari_geom ON public.bcn_arbrat_viari USING gist (geom);

---------------------------------------- Creacio de la taula d'arbres ----------------------------------------

CREATE TABLE public.bcn_arbrat_zona (
                                        codi text NULL,
                                        x_etrs89 float8 NULL,
                                        y_etrs89 float8 NULL,
                                        latitud float8 NULL,
                                        longitud float8 NULL,
                                        tipus_element text NULL,
                                        espai_verd text NULL,
                                        adreca text NULL,
                                        cat_especie_id int8 NULL,
                                        cat_nom_cientific text NULL,
                                        cat_nom_castella text NULL,
                                        cat_nom_catala text NULL,
                                        categoria_arbrat text NULL,
                                        data_plantacio text NULL,
                                        tipus_aigua text NULL,
                                        tipus_reg text NULL,
                                        geom public.geometry(point, 25831) NULL,
                                        catalogacio text NULL,
                                        codi_barri float8 NULL,
                                        nom_barri text NULL,
                                        codi_districte float8 NULL,
                                        nom_districte text NULL
);
CREATE INDEX idx_arbrat_zona_geom ON public.bcn_arbrat_zona USING gist (geom);

---------------------------------------- Creacio de la taula de bancs ----------------------------------------

CREATE TABLE public.bcn_bancs (
                                  gis_id int8 NULL,
                                  tipus_de_mobiliari_urba text NULL,
                                  codi text NULL,
                                  descripcio text NULL,
                                  fabricant text NULL,
                                  codi_districte int8 NULL,
                                  nom_districte text NULL,
                                  codi_barri int8 NULL,
                                  nom_barri text NULL,
                                  zona text NULL,
                                  nom_carrer text NULL,
                                  num_carrer text NULL,
                                  x_etrs89 float8 NULL,
                                  y_etrs89 float8 NULL,
                                  geometria_etrs89 text NULL,
                                  longitud float8 NULL,
                                  latitud float8 NULL,
                                  geometria_wgs84 text NULL,
                                  data_alta text NULL,
                                  data_baixa text NULL,
                                  geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_bancs_geom ON public.bcn_bancs USING gist (geom);

---------------------------------------- Creacio de la taula de cameres de seguretat ----------------------------------------

CREATE TABLE public.bcn_cameres_seguretat (
                                              id_cam_seguretat int8 NULL,
                                              codi_cam_seguretat text NULL,
                                              tipus_cam_seguretat text NULL,
                                              num_cam_seguretat text NULL,
                                              codi_suport text NULL,
                                              codi_districte int8 NULL,
                                              nom_districte text NULL,
                                              codi_barri int8 NULL,
                                              nom_barri text NULL,
                                              x_etrs89 int8 NULL,
                                              y_etrs89 int8 NULL,
                                              longitud float8 NULL,
                                              latitud float8 NULL,
                                              data_alta text NULL,
                                              geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_cameres_geom ON public.bcn_cameres_seguretat USING gist (geom);

---------------------------------------- Creacio de la taula de comissaries ----------------------------------------

CREATE TABLE public.bcn_comissaries (
                                        register_id text NULL,
                                        "name" text NULL,
                                        institution_id float8 NULL,
                                        institution_name float8 NULL,
                                        created text NULL,
                                        modified text NULL,
                                        addresses_roadtype_id float8 NULL,
                                        addresses_roadtype_name float8 NULL,
                                        addresses_road_id int8 NULL,
                                        addresses_road_name text NULL,
                                        addresses_start_street_number int8 NULL,
                                        addresses_end_street_number float8 NULL,
                                        addresses_neighborhood_id int8 NULL,
                                        addresses_neighborhood_name text NULL,
                                        addresses_district_id int8 NULL,
                                        addresses_district_name text NULL,
                                        addresses_zip_code int8 NULL,
                                        addresses_town text NULL,
                                        addresses_main_address bool NULL,
                                        addresses_type float8 NULL,
                                        values_id int8 NULL,
                                        values_attribute_id int8 NULL,
                                        values_category text NULL,
                                        values_attribute_name text NULL,
                                        values_value int8 NULL,
                                        values_outstanding bool NULL,
                                        values_description text NULL,
                                        secondary_filters_id int8 NULL,
                                        secondary_filters_name text NULL,
                                        secondary_filters_fullpath text NULL,
                                        secondary_filters_tree int8 NULL,
                                        secondary_filters_asia_id int8 NULL,
                                        geo_epgs_25831_x float8 NULL,
                                        geo_epgs_25831_y float8 NULL,
                                        geo_epgs_4326_lat float8 NULL,
                                        geo_epgs_4326_lon float8 NULL,
                                        estimated_dates float8 NULL,
                                        start_date float8 NULL,
                                        end_date float8 NULL,
                                        timetable float8 NULL,
                                        geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_comissaries_geom ON public.bcn_comissaries USING gist (geom);

---------------------------------------- Creacio de la taula de contaminacio acustica ----------------------------------------

CREATE TABLE public.bcn_contaminacio_acustica (
                                                  latitud float8 NULL,
                                                  longitud float8 NULL,
                                                  nivell_db float8 NULL,
                                                  geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_soroll_geom ON public.bcn_contaminacio_acustica USING gist (geom);

---------------------------------------- Creacio de la taula de separacio de bcn en poligons ----------------------------------------

CREATE TABLE public.bcn_districtes_poligons (
                                                gid serial4 NOT NULL,
                                                id_annex varchar(5) NULL,
                                                annexdescr varchar(50) NULL,
                                                id_tema varchar(10) NULL,
                                                tema_descr varchar(50) NULL,
                                                id_conjunt varchar(15) NULL,
                                                conj_descr varchar(50) NULL,
                                                id_subconj varchar(20) NULL,
                                                sconj_desc varchar(50) NULL,
                                                id_element varchar(25) NULL,
                                                elem_descr varchar(50) NULL,
                                                nivell varchar(35) NULL,
                                                ndescr_ca varchar(100) NULL,
                                                ndescr_es varchar(100) NULL,
                                                ndescr_en varchar(100) NULL,
                                                terme varchar(6) NULL,
                                                districte varchar(2) NULL,
                                                barri varchar(2) NULL,
                                                aeb varchar(3) NULL,
                                                sec_cens varchar(3) NULL,
                                                granbarri varchar(2) NULL,
                                                zua varchar(2) NULL,
                                                area_i varchar(1) NULL,
                                                literal varchar(15) NULL,
                                                perimetre numeric NULL,
                                                area numeric NULL,
                                                ord_repres int4 NULL,
                                                codi_ua varchar(6) NULL,
                                                tipus_ua varchar(15) NULL,
                                                nom varchar(60) NULL,
                                                web1 varchar(254) NULL,
                                                web2 varchar(254) NULL,
                                                web3 varchar(254) NULL,
                                                documenta varchar(254) NULL,
                                                rangescala varchar(12) NULL,
                                                tipus_pol varchar(5) NULL,
                                                gruix_id varchar(2) NULL,
                                                gruixdimen numeric NULL,
                                                estil_id varchar(2) NULL,
                                                estil_qgis varchar(20) NULL,
                                                valor1qgis varchar(20) NULL,
                                                valor2qgis varchar(20) NULL,
                                                col_farcit varchar(3) NULL,
                                                fcol_descr varchar(50) NULL,
                                                fhex_color varchar(7) NULL,
                                                col_descr varchar(50) NULL,
                                                hex_color7 varchar(7) NULL,
                                                geom public.geometry(multipolygon, 25831) NULL,
                                                CONSTRAINT bcn_districtes_poligons_pkey PRIMARY KEY (gid)
);
CREATE INDEX bcn_districtes_poligons_geom_idx ON public.bcn_districtes_poligons USING gist (geom);

---------------------------------------- Creacio de la taula d'escales mecaniques ----------------------------------------

CREATE TABLE public.bcn_escales_mecaniques (
                                               register_id text NULL,
                                               "name" text NULL,
                                               institution_id float8 NULL,
                                               institution_name float8 NULL,
                                               created text NULL,
                                               modified text NULL,
                                               addresses_roadtype_id float8 NULL,
                                               addresses_roadtype_name float8 NULL,
                                               addresses_road_id int8 NULL,
                                               addresses_road_name text NULL,
                                               addresses_start_street_number int8 NULL,
                                               addresses_end_street_number float8 NULL,
                                               addresses_neighborhood_id int8 NULL,
                                               addresses_neighborhood_name text NULL,
                                               addresses_district_id int8 NULL,
                                               addresses_district_name text NULL,
                                               addresses_zip_code int8 NULL,
                                               addresses_town text NULL,
                                               addresses_main_address bool NULL,
                                               addresses_type float8 NULL,
                                               values_id float8 NULL,
                                               values_attribute_id float8 NULL,
                                               values_category text NULL,
                                               values_attribute_name text NULL,
                                               values_value text NULL,
                                               values_outstanding bool NULL,
                                               values_description float8 NULL,
                                               secondary_filters_id int8 NULL,
                                               secondary_filters_name text NULL,
                                               secondary_filters_fullpath text NULL,
                                               secondary_filters_tree int8 NULL,
                                               secondary_filters_asia_id int8 NULL,
                                               geo_epgs_25831_x float8 NULL,
                                               geo_epgs_25831_y float8 NULL,
                                               geo_epgs_4326_lat float8 NULL,
                                               geo_epgs_4326_lon float8 NULL,
                                               estimated_dates float8 NULL,
                                               start_date float8 NULL,
                                               end_date float8 NULL,
                                               timetable float8 NULL,
                                               geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_escales_geom ON public.bcn_escales_mecaniques USING gist (geom);

---------------------------------------- Creacio de la taula de fonts de beure ----------------------------------------

CREATE TABLE public.bcn_fonts_beure (
                                        "ï»¿""codi""" text NULL,
                                        nom text NULL,
                                        carrer text NULL,
                                        numero_carrer text NULL,
                                        codi_barri float8 NULL,
                                        nom_barri text NULL,
                                        codi_districte int8 NULL,
                                        nom_districte text NULL,
                                        x_etrs89 float8 NULL,
                                        y_etrs89 float8 NULL,
                                        latitud float8 NULL,
                                        longitud float8 NULL,
                                        geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_fonts_geom ON public.bcn_fonts_beure USING gist (geom);

---------------------------------------- Creacio de la taula dels nodes de la via ----------------------------------------

CREATE TABLE public.bcn_grafvial_nodes (
                                           "FID" int8 NULL,
                                           "C_Nus" text NULL,
                                           "Coord_X" float8 NULL,
                                           "Coord_Y" float8 NULL
);
CREATE INDEX idx_grafvial_nodes_cnus ON public.bcn_grafvial_nodes USING btree ("C_Nus");

---------------------------------------- Creacio de la taula dels trams de les rutes i vies ----------------------------------------

CREATE TABLE public.bcn_grafvial_trams (
                                           "FID" int8 NULL,
                                           "COORD_X" float8 NULL,
                                           "COORD_Y" float8 NULL,
                                           "LONGITUD" float8 NULL,
                                           "ANGLE" float8 NULL,
                                           "C_Tram" text NULL,
                                           "C_Nus_I" text NULL,
                                           "C_Nus_F" text NULL,
                                           "Distric_D" int8 NULL,
                                           "NDistric_D" text NULL,
                                           "Illa_D" int8 NULL,
                                           "CVia_D" int8 NULL,
                                           "TVia_D" text NULL,
                                           "NVia_D" text NULL,
                                           "Tram_Dret" text NULL,
                                           "Distric_E" int8 NULL,
                                           "NDistric_E" text NULL,
                                           "Illa_E" int8 NULL,
                                           "CVia_E" int8 NULL,
                                           "TVia_E" text NULL,
                                           "NVia_E" text NULL,
                                           "Tram_Esq" text NULL,
                                           cnt_comissaries int4 DEFAULT 0 NULL,
                                           cnt_fonts int4 DEFAULT 0 NULL,
                                           cnt_bancs int4 DEFAULT 0 NULL,
                                           cnt_cameres int4 DEFAULT 0 NULL,
                                           cnt_escales int4 DEFAULT 0 NULL,
                                           cnt_arbres int4 DEFAULT 0 NULL,
                                           score_comissaries numeric DEFAULT 0 NULL,
                                           cnt_fets_delictius numeric DEFAULT 0 NULL,
                                           cnt_infraccions numeric DEFAULT 0 NULL,
                                           score_soroll numeric DEFAULT 0 NULL,
                                           score_aire numeric DEFAULT 0 NULL,
                                           cnt_refugis_climatics int4 DEFAULT 0 NULL,
                                           cnt_incidents int4 DEFAULT 0 NULL,
                                           geom public.geometry(linestring, 25831) NULL
);
CREATE INDEX idx_grafvial_trams_nf ON public.bcn_grafvial_trams USING btree ("C_Nus_F");
CREATE INDEX idx_grafvial_trams_ni ON public.bcn_grafvial_trams USING btree ("C_Nus_I");
CREATE INDEX idx_trams_geom ON public.bcn_grafvial_trams USING gist (geom);

---------------------------------------- Creacio de la taula de la qualitat de l'aire ----------------------------------------

CREATE TABLE public.bcn_qualitat_aire (
                                          latitud float8 NULL,
                                          longitud float8 NULL,
                                          nivell_aire float8 NULL,
                                          geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_aire_geom ON public.bcn_qualitat_aire USING gist (geom);

---------------------------------------- Creacio de la taula de refugis climatics ----------------------------------------

CREATE TABLE public.bcn_refugis_climatics (
                                              register_id text NULL,
                                              "name" text NULL,
                                              institution_id float8 NULL,
                                              institution_name text NULL,
                                              created text NULL,
                                              modified text NULL,
                                              addresses_roadtype_id float8 NULL,
                                              addresses_roadtype_name float8 NULL,
                                              addresses_road_id int8 NULL,
                                              addresses_road_name text NULL,
                                              addresses_start_street_number float8 NULL,
                                              addresses_end_street_number float8 NULL,
                                              addresses_neighborhood_id int8 NULL,
                                              addresses_neighborhood_name text NULL,
                                              addresses_district_id int8 NULL,
                                              addresses_district_name text NULL,
                                              addresses_zip_code int8 NULL,
                                              addresses_town text NULL,
                                              addresses_main_address bool NULL,
                                              addresses_type float8 NULL,
                                              values_id float8 NULL,
                                              values_attribute_id float8 NULL,
                                              values_category text NULL,
                                              values_attribute_name text NULL,
                                              values_value text NULL,
                                              values_outstanding bool NULL,
                                              values_description float8 NULL,
                                              geo_epgs_25831_x float8 NULL,
                                              geo_epgs_25831_y float8 NULL,
                                              geo_epgs_4326_lat float8 NULL,
                                              geo_epgs_4326_lon float8 NULL,
                                              timetable text NULL,
                                              geom public.geometry(point, 25831) NULL
);
CREATE INDEX idx_refugis_geom ON public.bcn_refugis_climatics USING gist (geom);

---------------------------------------- Creacio de la taula de fets penals ----------------------------------------

CREATE TABLE public.cat_fets_penals (
                                        nom_districte text NULL,
                                        total_delictes_vianants int8 NULL
);

---------------------------------------- Creacio de la taula d'infraccions ----------------------------------------

CREATE TABLE public.cat_infraccions_joc (
                                            nom_districte text NULL,
                                            nombre_fets_o_infraccions int8 NULL,
                                            nombre_victimes float8 NULL
);