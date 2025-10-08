CREATE TABLE IF NOT EXISTS vedtak
(
    vedtak_id         VARCHAR(255) PRIMARY KEY,
    sak_id            VARCHAR(255) NOT NULL,
    ident             VARCHAR(11) NOT NULL,
    utfall            VARCHAR(10)  NOT NULL,
    stonad_type       VARCHAR(36)  NOT NULL,
    fra_og_med_dato   TIMESTAMP    NOT NULL,
    til_og_med_dato   TIMESTAMP,
    dagsats           INTEGER,
    barnetillegg      INTEGER,
    kilde             VARCHAR(5)   NOT NULL,
    tidspunkt         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS vedtak_sak_id_idx ON vedtak (sak_id);
CREATE INDEX IF NOT EXISTS vedtak_ident_idx ON vedtak (ident);
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE INDEX IF NOT EXISTS vedtak_fra_og_med_dato_gist_idx ON vedtak USING gist (fra_og_med_dato);
CREATE INDEX IF NOT EXISTS vedtak_til_og_med_dato_gist_idx ON vedtak USING gist (til_og_med_dato);
CREATE INDEX IF NOT EXISTS vedtak_tidspunkt_gist_idx ON vedtak USING gist (tidspunkt);
