-- Denetim kaydı tablosu (T-017).
-- Kim, ne zaman, neyi değiştirdi (6 olay: USER_CREATED, USER_UPDATED, USER_DELETED, LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT).
-- app_user'a Foreign Key konmaz: kullanıcı silindiğinde denetim geçmişi korunur.

CREATE TABLE public.app_audit_log (
    id bigserial PRIMARY KEY,
    occurred_at timestamptz NOT NULL,
    event varchar(30) NOT NULL,
    actor varchar(50) NULL,
    target varchar(50) NULL,
    old_value text NULL,
    new_value text NULL
);

-- Denetim kaydı en yeniden eskiye okunur (ENG-08 §2.1)
CREATE INDEX idx_app_audit_log_occurred_at_desc ON public.app_audit_log (occurred_at DESC);

-- Kullanıcı adına göre arama performansı için indeksler
CREATE INDEX idx_app_audit_log_actor ON public.app_audit_log (actor);
CREATE INDEX idx_app_audit_log_target ON public.app_audit_log (target);
