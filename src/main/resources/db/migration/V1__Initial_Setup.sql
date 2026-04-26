-- Initial database setup for the incident reporting application
-- Votes table definition
CREATE TABLE public.votes (
                              id bigserial NOT NULL,
                              incidence_id int8 NULL,
                              google_id varchar(255) NULL,
                              score float8 NOT NULL,
                              user_reliability float8 NOT NULL,
                              data_score float8 NOT NULL,
                              created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                              user_id int8 NULL,
                              CONSTRAINT votes_incidence_id_user_id_key UNIQUE (incidence_id, google_id),
                              CONSTRAINT votes_pkey PRIMARY KEY (id)
                              CONSTRAINT fk_google_id_votes_users FOREIGN KEY (google_id) REFERENCES public.users(google_id);
                              CONSTRAINT votes_incidence_id_fkey FOREIGN KEY (incidence_id) REFERENCES public.incidents(id) ON DELETE CASCADE;
);






-- public.users definition

CREATE TABLE public.users (
                              id bigserial NOT NULL,
                              google_id varchar(100) NOT NULL,
                              email varchar(100) NOT NULL,
                              username varchar(50) NOT NULL,
                              picture_url varchar(255) NULL,
                              "language" varchar(10) DEFAULT 'ca'::character varying NULL,
                              points int4 DEFAULT 0 NULL,
                              "level" int8 DEFAULT 1 NULL,
                              is_anonymous bool DEFAULT false NULL,
                              created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                              updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                              reputacio int4 DEFAULT 1 NOT NULL,
                              status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
                              CONSTRAINT users_email_key UNIQUE (email),
                              CONSTRAINT users_google_id_key UNIQUE (google_id),
                              CONSTRAINT users_pkey PRIMARY KEY (id),
                              CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'BANNED'::character varying])::text[])))
);



-- public.incidents definition

CREATE TABLE public.incidents (
                                  id bigserial NOT NULL,
                                  "type" varchar(50) NOT NULL,
                                  description text NULL,
                                  "location" public.geometry(point, 4326) NOT NULL,
                                  positive_votes int4 DEFAULT 0 NULL,
                                  negative_votes int4 DEFAULT 0 NULL,
                                  reliability_index float8 DEFAULT 0.0 NULL,
                                  status varchar(20) DEFAULT 'PENDING'::character varying NULL,
                                  created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                                  updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                                  google_id varchar(255) NULL,
                                  user_id int8 NULL,
                                  CONSTRAINT incidents_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_incidents_location ON public.incidents USING gist (location);


-- public.incidents foreign keys

ALTER TABLE public.incidents ADD CONSTRAINT incidents_google_id_fkey FOREIGN KEY (google_id) REFERENCES public.users(google_id) ON DELETE CASCADE;




-- public.filtres definition

CREATE TABLE public.filtres (
                                google_id varchar(255) NOT NULL,
                                comissaries float8 DEFAULT 0.5 NULL,
                                fetspenals float8 DEFAULT 0.5 NULL,
                                cameresseguretat float8 DEFAULT 0.5 NULL,
                                infraccions float8 DEFAULT 0.5 NULL,
                                fontsaigua float8 DEFAULT 0.5 NULL,
                                bancs float8 DEFAULT 0.5 NULL,
                                contaminacioacustica float8 DEFAULT 0.5 NULL,
                                escalesmecaniques float8 DEFAULT 0.5 NULL,
                                arbres float8 DEFAULT 0.5 NULL,
                                refugisclimatics float8 DEFAULT 0.5 NULL,
                                qualitataire float8 DEFAULT 0.5 NULL,
                                cameres_seguretat float8 DEFAULT 0 NULL,
                                contaminacio_acustica float8 DEFAULT 0 NULL,
                                escales_mecaniques float8 DEFAULT 0 NULL,
                                fets_penals float8 DEFAULT 0 NULL,
                                fonts_aigua float8 DEFAULT 0 NULL,
                                qualitat_aire float8 DEFAULT 0 NULL,
                                refugis_climatics float8 DEFAULT 0 NULL,
                                CONSTRAINT filtres_arbres_check CHECK (((arbres >= (0)::double precision) AND (arbres <= (1)::double precision))),
                                CONSTRAINT filtres_bancs_check CHECK (((bancs >= (0)::double precision) AND (bancs <= (1)::double precision))),
                                CONSTRAINT filtres_cameres_seguretat_check CHECK (((cameres_seguretat >= (0)::double precision) AND (cameres_seguretat <= (1)::double precision))),
                                CONSTRAINT filtres_cameresseguretat_check CHECK (((cameresseguretat >= (0)::double precision) AND (cameresseguretat <= (1)::double precision))),
                                CONSTRAINT filtres_comissaries_check CHECK (((comissaries >= (0)::double precision) AND (comissaries <= (1)::double precision))),
                                CONSTRAINT filtres_contaminacio_acustica_check CHECK (((contaminacio_acustica >= (0)::double precision) AND (contaminacio_acustica <= (1)::double precision))),
                                CONSTRAINT filtres_contaminacioacustica_check CHECK (((contaminacioacustica >= (0)::double precision) AND (contaminacioacustica <= (1)::double precision))),
                                CONSTRAINT filtres_escales_mecaniques_check CHECK (((escales_mecaniques >= (0)::double precision) AND (escales_mecaniques <= (1)::double precision))),
                                CONSTRAINT filtres_escalesmecaniques_check CHECK (((escalesmecaniques >= (0)::double precision) AND (escalesmecaniques <= (1)::double precision))),
                                CONSTRAINT filtres_fets_penals_check CHECK (((fets_penals >= (0)::double precision) AND (fets_penals <= (1)::double precision))),
                                CONSTRAINT filtres_fetspenals_check CHECK (((fetspenals >= (0)::double precision) AND (fetspenals <= (1)::double precision))),
                                CONSTRAINT filtres_fonts_aigua_check CHECK (((fonts_aigua >= (0)::double precision) AND (fonts_aigua <= (1)::double precision))),
                                CONSTRAINT filtres_fontsaigua_check CHECK (((fontsaigua >= (0)::double precision) AND (fontsaigua <= (1)::double precision))),
                                CONSTRAINT filtres_infraccions_check CHECK (((infraccions >= (0)::double precision) AND (infraccions <= (1)::double precision))),
                                CONSTRAINT filtres_pkey PRIMARY KEY (google_id),
                                CONSTRAINT filtres_qualitat_aire_check CHECK (((qualitat_aire >= (0)::double precision) AND (qualitat_aire <= (1)::double precision))),
                                CONSTRAINT filtres_qualitataire_check CHECK (((qualitataire >= (0)::double precision) AND (qualitataire <= (1)::double precision))),
                                CONSTRAINT filtres_refugis_climatics_check CHECK (((refugis_climatics >= (0)::double precision) AND (refugis_climatics <= (1)::double precision))),
                                CONSTRAINT filtres_refugisclimatics_check CHECK (((refugisclimatics >= (0)::double precision) AND (refugisclimatics <= (1)::double precision)))
);


-- public.filtres foreign keys

ALTER TABLE public.filtres ADD CONSTRAINT filtres_google_id_fkey FOREIGN KEY (google_id) REFERENCES public.users(google_id) ON DELETE CASCADE;