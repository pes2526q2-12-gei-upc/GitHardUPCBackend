---------------------------------------- Creacio de la taula d'usuaris ----------------------------------------
CREATE TABLE public.users (
                              id bigserial NOT NULL,
                              google_id varchar(100) NOT NULL,
                              email varchar(100) NOT NULL,
                              username varchar(50) NOT NULL,
                              picture_url varchar(255) NULL,
                              "language" varchar(10) DEFAULT 'ca'::character varying NULL,
                              points int8 DEFAULT 0 NULL,
                              "level" int8 DEFAULT 1 NULL,
                              is_anonymous bool DEFAULT false NULL,
                              created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                              updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                              reputacio float8 DEFAULT 1 NOT NULL,
                              status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
                              pending_rewards int8 DEFAULT 0 NULL,
                              fcm_token varchar(255) NULL,
                              is_online bool DEFAULT false NOT NULL,
                              is_in_emergency bool DEFAULT false NOT NULL,
                              CONSTRAINT users_email_key UNIQUE (email),
                              CONSTRAINT users_google_id_key UNIQUE (google_id),
                              CONSTRAINT users_pkey PRIMARY KEY (id),
                              CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'BANNED'::character varying])::text[])))
);

---------------------------------------- Creacio de la taula de contactes d'emergencia ----------------------------------------

CREATE TABLE public.emergency_contacts (
                                           user_google_id varchar(100) NOT NULL,
                                           emergency_contact_google_id varchar(100) NOT NULL,
                                           CONSTRAINT emergency_contacts_check CHECK (((user_google_id)::text <> (emergency_contact_google_id)::text)),
	CONSTRAINT emergency_contacts_pkey PRIMARY KEY (user_google_id, emergency_contact_google_id)
);


-- public.emergency_contacts foreign keys

ALTER TABLE public.emergency_contacts ADD CONSTRAINT emergency_contacts_emergency_contact_google_id_fkey FOREIGN KEY (emergency_contact_google_id) REFERENCES public.users(google_id);
ALTER TABLE public.emergency_contacts ADD CONSTRAINT emergency_contacts_user_google_id_fkey FOREIGN KEY (user_google_id) REFERENCES public.users(google_id);

---------------------------------------- Creacio de la taula de filtres ----------------------------------------

CREATE TABLE public.filtres (
                                google_id varchar(255) NOT NULL,
                                comissaries float8 DEFAULT 0.5 NULL,
                                infraccions float8 DEFAULT 0.5 NULL,
                                bancs float8 DEFAULT 0.5 NULL,
                                arbres float8 DEFAULT 0.5 NULL,
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
                                CONSTRAINT filtres_comissaries_check CHECK (((comissaries >= (0)::double precision) AND (comissaries <= (1)::double precision))),
                                CONSTRAINT filtres_contaminacio_acustica_check CHECK (((contaminacio_acustica >= (0)::double precision) AND (contaminacio_acustica <= (1)::double precision))),
                                CONSTRAINT filtres_escales_mecaniques_check CHECK (((escales_mecaniques >= (0)::double precision) AND (escales_mecaniques <= (1)::double precision))),
                                CONSTRAINT filtres_fets_penals_check CHECK (((fets_penals >= (0)::double precision) AND (fets_penals <= (1)::double precision))),
                                CONSTRAINT filtres_fonts_aigua_check CHECK (((fonts_aigua >= (0)::double precision) AND (fonts_aigua <= (1)::double precision))),
                                CONSTRAINT filtres_infraccions_check CHECK (((infraccions >= (0)::double precision) AND (infraccions <= (1)::double precision))),
                                CONSTRAINT filtres_pkey PRIMARY KEY (google_id),
                                CONSTRAINT filtres_qualitat_aire_check CHECK (((qualitat_aire >= (0)::double precision) AND (qualitat_aire <= (1)::double precision))),
                                CONSTRAINT filtres_refugis_climatics_check CHECK (((refugis_climatics >= (0)::double precision) AND (refugis_climatics <= (1)::double precision)))
);


-- public.filtres foreign keys

ALTER TABLE public.filtres ADD CONSTRAINT filtres_google_id_fkey FOREIGN KEY (google_id) REFERENCES public.users(google_id) ON DELETE CASCADE;

---------------------------------------- Creacio de la taula d'amistats ----------------------------------------

CREATE TABLE public.friendships (
                                    id bigserial NOT NULL,
                                    sender_id int8 NOT NULL,
                                    receiver_id int8 NOT NULL,
                                    status varchar(20) DEFAULT 'PENDING'::character varying NOT NULL,
                                    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    CONSTRAINT chk_no_self_friendship CHECK ((sender_id <> receiver_id)),
                                    CONSTRAINT chk_status_values CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'BLOCKED'::character varying])::text[]))),
	CONSTRAINT friendships_pkey PRIMARY KEY (id),
	CONSTRAINT uq_friendship UNIQUE (sender_id, receiver_id)
);
CREATE INDEX idx_friendships_receiver ON public.friendships USING btree (receiver_id);
CREATE INDEX idx_friendships_sender ON public.friendships USING btree (sender_id);


-- public.friendships foreign keys

ALTER TABLE public.friendships ADD CONSTRAINT fk_friendship_receiver FOREIGN KEY (receiver_id) REFERENCES public.users(id) ON DELETE CASCADE;
ALTER TABLE public.friendships ADD CONSTRAINT fk_friendship_sender FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE;