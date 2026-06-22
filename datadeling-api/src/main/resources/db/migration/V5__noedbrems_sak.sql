-- Legger til kolonne som har informasjon om at saken er stoppet i dp-sak (nødbrems)
ALTER TABLE behandlingresultat ADD COLUMN IF NOT EXISTS nødbrems BOOLEAN NOT NULL DEFAULT FALSE;

-- Setter behandling som nødbremset ref - https://nav-it.slack.com/archives/C0A0XTM6Z37/p1782122087134559?thread_ts=1782109541.617569&cid=C0A0XTM6Z37
UPDATE  behandlingresultat
SET nødbrems = TRUE
WHERE behandling_id = '019e3cc8-47f0-749e-b603-4e0e5ba73a56'
  AND sak_id = '019e3cc8-47f0-749e-b603-4e0e5ba73a56';