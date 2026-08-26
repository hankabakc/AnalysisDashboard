-- Guvenlik semasi: Kullanicilar ve roller.
-- Bu tablolar yalnizca bu uygulama tarafindan yonetilir; PLC toplayici tablolara dokunulmaz.

CREATE TABLE public.app_user (
	username varchar(50) NOT NULL,
	password varchar(100) NOT NULL,
	enabled boolean NOT NULL DEFAULT true,
	CONSTRAINT app_user_pkey PRIMARY KEY (username)
);

CREATE TABLE public.app_user_role (
	username varchar(50) NOT NULL,
	role varchar(50) NOT NULL,
	CONSTRAINT app_user_role_pkey PRIMARY KEY (username, role),
	CONSTRAINT fk_app_user_role_user FOREIGN KEY (username) REFERENCES public.app_user(username) ON DELETE CASCADE
);

-- Varsayilan gelistirme kullanicilari (BCrypt karmalari: admin123, user123, apiuser123)
-- Parolalar duz metin saklanmaz.
INSERT INTO public.app_user (username, password, enabled) VALUES
('admin', '$2a$10$v4h.EHcRAOs2ELt17NmSwenjryMRZgI5uhzP65OvQ7pb/m81FMld6', true),
('user', '$2a$10$ZTsCYzJYjDuk5fTaKXTsF.XXrNkYfF0ITKUUcMwnMs/MbItWfDRIW', true),
('apiuser', '$2a$10$ylsX/wnSGSpOAIwnRXKzVOK5tBA..RwMYMiXQac0VcsbzOgUcB1p2', true);

INSERT INTO public.app_user_role (username, role) VALUES
('admin', 'ADMIN'),
('user', 'USER'),
('apiuser', 'APIUSER');
