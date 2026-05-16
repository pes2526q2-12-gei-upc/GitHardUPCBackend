---------------------------------------- Creacio de la taula de premis ----------------------------------------
CREATE TABLE public.prizes (
                               id varchar(255) NOT NULL,
                               "name" varchar(255) NOT NULL,
                               url varchar(255) NULL,
                               probability float8 NULL,
                               CONSTRAINT prizes_pkey PRIMARY KEY (id),
                               CONSTRAINT prizes_probability_check CHECK (((probability <= (1)::double precision) AND (probability >= (0)::double precision)))
);

---------------------------------------- Creacio de la taula dels premis de cada usuari ----------------------------------------

CREATE TABLE public.user_prizes (
                                    id varchar(255) NOT NULL,
                                    google_id varchar(100) NOT NULL,
                                    CONSTRAINT user_prizes_pk PRIMARY KEY (id, google_id)
);


-- public.user_prizes foreign keys

ALTER TABLE public.user_prizes ADD CONSTRAINT fkp78soqcmp89rhxsob5mnjema3 FOREIGN KEY (id) REFERENCES public.prizes(id);
ALTER TABLE public.user_prizes ADD CONSTRAINT user_prizes_google_id_fkey FOREIGN KEY (google_id) REFERENCES public.users(google_id);


INSERT INTO public.prizes (id, name, url, probability) VALUES ('R001_RTCOL_R0G255B0', 'Ruta de color verd', 'NONE', 0.2);
INSERT INTO public.prizes (id, name, url, probability) VALUES ('R002_RTCOL_R255G192B203', 'Ruta de color rosa', 'NONE', 0.2);
INSERT INTO public.prizes (id, name, url, probability) VALUES ('R003_RTCOL_R200G162B200', 'Ruta de color lila', 'NONE', 0.2);
INSERT INTO public.prizes (id, name, url, probability) VALUES ('R004_RTCOL_R255G0B0', 'Ruta de color vermell', 'NONE', 0.2);