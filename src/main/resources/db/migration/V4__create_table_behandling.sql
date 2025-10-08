CREATE TABLE behandling
(
    behandling_id VARCHAR(36) PRIMARY KEY,
    soknad_id     VARCHAR(36) NOT NULL,
    ident         VARCHAR(11) NOT NULL,
    sak_id        VARCHAR(36) NOT NULL,
    tidspunkt     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX behandling_soknad_id_index ON behandling (soknad_id);
CREATE INDEX behandling_ident_index ON behandling (ident);
CREATE INDEX behandling_sak_id_index ON behandling (sak_id);
