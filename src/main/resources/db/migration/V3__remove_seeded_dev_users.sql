-- V2, bilinen parolali gelistirme kullanicilarini (admin, user, apiuser) HER ortama ekliyordu.
-- Uygulanmis bir migrasyon degistirilemez (Flyway checksum), bu yuzden o ekleme burada geri alinir.
-- Yalnizca parolasi hala V2'deki bilinen deger olan hesaplar silinir; parolasi degistirilmis hesaba dokunulmaz.
-- Rolleri ON DELETE CASCADE ile birlikte silinir.
-- Gelistirme ortaminda ve testlerde bu kullanicilar db/dev/afterMigrate__dev_users.sql ile yeniden eklenir.

DELETE FROM public.app_user
WHERE (username, password) IN (
    ('admin',   '$2a$10$v4h.EHcRAOs2ELt17NmSwenjryMRZgI5uhzP65OvQ7pb/m81FMld6'),
    ('user',    '$2a$10$ZTsCYzJYjDuk5fTaKXTsF.XXrNkYfF0ITKUUcMwnMs/MbItWfDRIW'),
    ('apiuser', '$2a$10$ylsX/wnSGSpOAIwnRXKzVOK5tBA..RwMYMiXQac0VcsbzOgUcB1p2')
);
