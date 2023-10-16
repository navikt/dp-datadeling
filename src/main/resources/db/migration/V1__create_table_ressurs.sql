DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ressurs_status') THEN
            CREATE TYPE ressurs_status AS ENUM ('opprettet', 'ferdig', 'feilet');
        END IF;
    END
$$;

CREATE TABLE IF NOT EXISTS ressurs
(
    id     serial primary key,
    status ressurs_status not null,
    data   json
);