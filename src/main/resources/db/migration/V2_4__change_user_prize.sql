INSERT INTO public.prizes (id, name, url, oddity) VALUES ('R008_RTCOL_R149G255B36', 'Ruta de color lima', 'NONE', 'RARE');

UPDATE public.user_prizes SET prize_id = 'R008_RTCOL_R149G255B36' WHERE prize_id = 'R008_RTCOL_R57G255B20';

DELETE FROM public.prizes WHERE id IN ('R008_RTCOL_R57G255B20');

