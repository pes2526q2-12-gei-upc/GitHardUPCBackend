ALTER TABLE public.shared_routes
ADD COLUMN route_type VARCHAR(50) NULL,
ADD COLUMN distance_meters DOUBLE PRECISION NULL,
ADD COLUMN duration_minutes INT NULL,
ADD COLUMN origin_address TEXT NULL,
ADD COLUMN dest_address TEXT NULL;

DELETE FROM public.prizes;

ALTER TABLE public.prizes DROP COLUMN probability;
ALTER TABLE public.prizes ADD COLUMN oddity VARCHAR(255) NOT NULL DEFAULT 'COMMON';


CREATE TABLE public.oddities (
    id varchar(255) NOT NULL,
    probability float NULL,
    percentage_lvl_compensation float NOT NULL DEFAULT 0.01,
    CONSTRAINT oddities_pkey PRIMARY KEY (id)
);

ALTER TABLE public.prizes ADD CONSTRAINT user_prizes_oddity_fkey FOREIGN KEY (oddity) REFERENCES public.oddities(id);


INSERT INTO public.oddities (id, probability, percentage_lvl_compensation) VALUES ('COMMON', 0.6, 0.05);
INSERT INTO public.oddities (id, probability, percentage_lvl_compensation) VALUES ('RARE', 0.3, 0.1);
INSERT INTO public.oddities (id, probability, percentage_lvl_compensation) VALUES ('EPIC', 0.09, 0.3);
INSERT INTO public.oddities (id, probability, percentage_lvl_compensation) VALUES ('LEGENDARY', 0.01, 0.8);

INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R001_RTCOL_R0G255B0', 'Ruta de color verd', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R002_RTCOL_R255G192B203', 'Ruta de color rosa', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R003_RTCOL_R200G162B200', 'Ruta de color lila', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R004_RTCOL_R255G0B0', 'Ruta de color vermell', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R005_RTCOL_R255G165B0', 'Ruta de color naranja ', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R006_RTCOL_R0G255B255', 'Ruta de color cian', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R007_RTCOL_R255G215B0', 'Ruta de color dorado ', 'NONE', 'COMMON');



INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R008_RTCOL_R57G255B20', 'Ruta de color lima  ', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R009_RTCOL_R255G127B80', 'Ruta de color coral', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R010_RTCOL_R0G206B209', 'Ruta de color turquesa ', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R011_RTCOL_R165G0B68 ', 'Ruta de color lima  ', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R012_RTCOL_R138G43B226 ', 'Ruta de color coral', 'NONE', 'RARE');



INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R013_RTPAT_BLAUGRANA', 'Ruta de color turquesa ', 'NONE', 'EPIC');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R014_RTPAT_TAXI ', 'Ruta de color lima  ', 'NONE', 'EPIC');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R015_RTPAT_NIT ', 'Ruta de color coral', 'NONE', 'EPIC');



INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R016_RTPAT_RGB_FLUID', 'Ruta de color turquesa ', 'NONE', 'LEGENDARY');



INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A001_AVT_BUS_TURISTIC', 'Avatar bus turistic', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A002_AVT_TAXI', 'Avatar taxi', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A003_AVT_BICING', 'Avatar bicing', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A004_AVT_TMB', 'Avatar tmb', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A005_AVT_COTORRA', 'Avatar cotorra', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A006_AVT_PANOT', 'Avatar pano', 'NONE', 'COMMON');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A007_AVT_PA_AMB_TOMAQUET', 'Avatar pa amb tomaquet', 'NONE', 'COMMON');



INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A008_AVT_ARC_TRIOMF', 'Avatar arc del triomf', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A009_AVT_TORRE_GLORIES', 'Avatar torre glories', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A010_AVT_HOTEL_W', 'Avatar hotel w', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A011_AVT_FONT_MAGICA', 'Avatar font magica', 'NONE', 'RARE');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A012_AVT_PARK_GUELL', 'Avatar parc guell', 'NONE', 'RARE');



INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A013_AVT_CASA_BATLLO', 'Avatar casa batllo', 'NONE', 'EPIC');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A014_AVT_CAMP_NOU', 'Avatar camp nou', 'NONE', 'EPIC');
INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A015_AVT_TIBIDABO', 'Avatar tibidabo', 'NONE', 'EPIC');



INSERT INTO public.prizes (id, name, url, oddity) VALUES ('A016_AVT_SAGRADA_FAMILIA', 'Avatar sagrada familia', 'NONE', 'LEGENDARY');
