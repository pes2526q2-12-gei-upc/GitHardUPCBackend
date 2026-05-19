---------------------------------------- Creacio de la taula de chats ----------------------------------------
CREATE TABLE public.chats (
                              id bigserial NOT NULL,
                              "type" varchar(20) NOT NULL,
                              "name" varchar(100) NULL,
                              created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                              CONSTRAINT chats_pkey PRIMARY KEY (id)
);

---------------------------------------- Creacio de la taula dels premis de participants ----------------------------------------

CREATE TABLE public.chat_participants (
                                          id bigserial NOT NULL,
                                          chat_id int8 NOT NULL,
                                          user_id int8 NOT NULL,
                                          joined_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                                          "role" varchar(255) DEFAULT NULL::character varying NULL,
                                          CONSTRAINT chat_participants_chat_id_user_id_key UNIQUE (chat_id, user_id),
                                          CONSTRAINT chat_participants_pkey PRIMARY KEY (id)
);


-- public.chat_participants foreign keys

ALTER TABLE public.chat_participants ADD CONSTRAINT chat_participants_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chats(id) ON DELETE CASCADE;
ALTER TABLE public.chat_participants ADD CONSTRAINT chat_participants_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

---------------------------------------- Creacio de la taula dels premis de missatges ----------------------------------------

CREATE TABLE public.messages (
                                 id bigserial NOT NULL,
                                 chat_id int8 NOT NULL,
                                 sender_id int8 NOT NULL,
                                 "content" text NOT NULL,
                                 created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                                 CONSTRAINT messages_pkey PRIMARY KEY (id)
);


-- public.messages foreign keys

ALTER TABLE public.messages ADD CONSTRAINT messages_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chats(id) ON DELETE CASCADE;
ALTER TABLE public.messages ADD CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE;

---------------------------------------- Creacio de la taula dels premis d'estat dels missatges ----------------------------------------

CREATE TABLE public.message_read_status (
                                            message_id int8 NOT NULL,
                                            user_id int8 NOT NULL,
                                            read_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                                            CONSTRAINT message_read_status_pkey PRIMARY KEY (message_id, user_id)
);


-- public.message_read_status foreign keys

ALTER TABLE public.message_read_status ADD CONSTRAINT message_read_status_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.messages(id) ON DELETE CASCADE;
ALTER TABLE public.message_read_status ADD CONSTRAINT message_read_status_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

---------------------------------------- Creacio de la taula dels premis de les rutes compartides ----------------------------------------

CREATE TABLE public.shared_routes (
                                      id bigserial NOT NULL,
                                      message_id int8 NOT NULL,
                                      origin_lat float8 NOT NULL,
                                      origin_lng float8 NOT NULL,
                                      dest_lat float8 NOT NULL,
                                      dest_lng float8 NOT NULL,
                                      scheduled_date timestamptz NULL,
                                      CONSTRAINT shared_routes_pkey PRIMARY KEY (id)
);


-- public.shared_routes foreign keys

ALTER TABLE public.shared_routes ADD CONSTRAINT fk_shared_routes_message FOREIGN KEY (message_id) REFERENCES public.messages(id) ON DELETE CASCADE;

