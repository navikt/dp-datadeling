CREATE TABLE IF NOT EXISTS soknad
(
    id             BIGSERIAL PRIMARY KEY,
    ident          CHAR(11),
    soknad_id      TEXT,
    journalpost_id VARCHAR(255),
    skjema_kode    VARCHAR(20),
    soknads_type   VARCHAR(20) NOT NULL,
    kanal          VARCHAR(20) NOT NULL,
    dato_innsendt  TIMESTAMP   NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS soknad_id_uindex ON soknad (soknad_id);
CREATE UNIQUE INDEX IF NOT EXISTS journalpost_id_uindex ON soknad (journalpost_id);
CREATE INDEX IF NOT EXISTS soknad_ident_idx ON soknad (ident);
CREATE INDEX IF NOT EXISTS soknad_dato_innsendt_idx ON soknad (dato_innsendt);
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE INDEX IF NOT EXISTS soknad_dato_innsendt_gist_idx ON soknad USING gist (dato_innsendt);
