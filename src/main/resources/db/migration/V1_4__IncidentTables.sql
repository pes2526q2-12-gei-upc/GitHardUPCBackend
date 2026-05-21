---------------------------------------- Creacio de la taula d'incidencies ----------------------------------------
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
                                  expiration_index float8 DEFAULT 0.0 NOT NULL,
                                  CONSTRAINT incidents_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_incidents_geom_25831 ON public.incidents USING gist (st_transform(location, 25831));
CREATE INDEX idx_incidents_location ON public.incidents USING gist (location);


-- public.incidents foreign keys

ALTER TABLE public.incidents ADD CONSTRAINT incidents_google_id_fkey FOREIGN KEY (google_id) REFERENCES public.users(google_id) ON DELETE CASCADE;

---------------------------------------- Creacio de la taula de vots ----------------------------------------

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
);


-- public.votes foreign keys

ALTER TABLE public.votes ADD CONSTRAINT fk_google_id_votes_users FOREIGN KEY (google_id) REFERENCES public.users(google_id);
ALTER TABLE public.votes ADD CONSTRAINT votes_incidence_id_fkey FOREIGN KEY (incidence_id) REFERENCES public.incidents(id) ON DELETE CASCADE;