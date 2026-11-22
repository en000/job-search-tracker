alter table app_settings
    add column theme text not null default 'LIGHT' check (theme in ('LIGHT', 'DARK'));
