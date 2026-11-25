-- Tilstand for søknadsbehandlinger (opprettet/ferdig/avbrutt), uavhengig av soknad-tabellen.
-- En søknad kan ha flere behandlinger (1—*), derfor er behandling_id primærnøkkel og soknad_id en vanlig kolonne.
-- utfall er førteTil fra søknadsbehandling_ferdig.
-- sak_id = behandlingsskjedeId. opprettet er tidspunktet søknadsbehandlingen ble opprettet.
CREATE TABLE IF NOT EXISTS soknadsbehandling
(
    behandling_id uuid        PRIMARY KEY,
    soknad_id     uuid        NOT NULL,
    sak_id        uuid        NOT NULL,
    ident         CHAR(11)    NOT NULL,
    opprettet     TIMESTAMP   NOT NULL,
    tilstand      VARCHAR(20) NOT NULL DEFAULT 'UnderBehandling',
    sist_endret   TIMESTAMP   NOT NULL,
    utfall        VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS soknadsbehandling_ident_idx ON soknadsbehandling (ident);
CREATE INDEX IF NOT EXISTS soknadsbehandling_sak_idx ON soknadsbehandling (sak_id);
CREATE INDEX IF NOT EXISTS soknadsbehandling_soknad_idx ON soknadsbehandling (soknad_id);
