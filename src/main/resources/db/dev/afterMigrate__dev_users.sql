-- Gelistirme kullanicilari. YALNIZCA dev profilinde ve testlerde calisir
-- (spring.flyway.locations icinde classpath:db/dev oldugunda). Canli ortama hic yuklenmez.
--
-- Bu bir Flyway "afterMigrate" geri cagrisidir: her acilista migrasyonlardan sonra calisir,
-- gecmis tablosuna yazilmaz. ON CONFLICT sayesinde tekrar tekrar calismasi zararsizdir.
--
-- Parolalar BCrypt karmasidir: admin123, user123, apiuser123

INSERT INTO public.app_user (username, password, enabled) VALUES
('admin',   '$2a$10$v4h.EHcRAOs2ELt17NmSwenjryMRZgI5uhzP65OvQ7pb/m81FMld6', true),
('user',    '$2a$10$ZTsCYzJYjDuk5fTaKXTsF.XXrNkYfF0ITKUUcMwnMs/MbItWfDRIW', true),
('apiuser', '$2a$10$ylsX/wnSGSpOAIwnRXKzVOK5tBA..RwMYMiXQac0VcsbzOgUcB1p2', true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO public.app_user_role (username, role) VALUES
('admin',   'ADMIN'),
('user',    'USER'),
('apiuser', 'APIUSER')
ON CONFLICT (username, role) DO NOTHING;
