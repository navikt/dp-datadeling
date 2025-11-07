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
    id        serial primary key,
    uuid      uuid           not null,
    status    ressurs_status not null,
    response  json,
    request   json           not null,
    opprettet timestamp      not null default now()
);

CREATE TABLE IF NOT EXISTS feilmelding
(
    id        serial primary key,
    melding   text,
    opprettet timestamp not null default now(),
    ressursId bigint    not null references ressurs (id)
);