CREATE TABLE IF NOT EXISTS behandlingresultat
(
    id             uuid        PRIMARY KEY default gen_random_uuid(),
    ident          CHAR(11)    NOT NULL,
    behandling_id  uuid        NOT NULL,
    sak_id         uuid        NOT NULL,
    json_data      JSONB       NOT NULL,
    opprettet      TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS resultat_ident_idx ON behandlingresultat (ident);
CREATE INDEX IF NOT EXISTS resultat_behandling_idx ON behandlingresultat (behandling_id);