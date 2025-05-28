CREATE TABLE IF NOT EXISTS vedtak
(
    id        BIGSERIAL PRIMARY KEY,
    ident     CHAR(11),
    vedtak_id VARCHAR(12) NOT NULL,
    fagsak_id VARCHAR(12) NOT NULL,
    status    VARCHAR(20) NOT NULL,
    fattet    TIMESTAMP   NOT NULL,
    fra_dato  TIMESTAMP   NOT NULL,
    til_dato  TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS vedtak_id_uindex ON vedtak (vedtak_id);

CREATE INDEX IF NOT EXISTS vedtak_ident_idx ON vedtak (ident);
CREATE INDEX IF NOT EXISTS vedtak_fattet_idx ON vedtak (fattet);
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE INDEX IF NOT EXISTS vedtak_fattet_gist_idx ON vedtak USING gist (fattet);
