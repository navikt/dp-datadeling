# dp-datadeling

REST-API som deler dagpengedata med andre systemer i og utenfor Nav: rettighetsperioder, beregninger, meldekort og dagpengestatus. Appen eies av teamdagpenger og kjører på Nais (GCP).

## API-dokumentasjon

API-et er beskrevet i OpenAPI. Les den publiserte dokumentasjonen på <https://navikt.github.io/dp-datadeling/openapi.html>. Kilden ligger i [`openapi/src/main/resources/datadeling-api.yaml`](openapi/src/main/resources/datadeling-api.yaml).

| Endepunkt (`/dagpenger/datadeling/v1/…`) | Brukes til | Autentisering |
|---|---|---|
| `POST /periode`, `GET /periode` | Eksterne henter rettighetsperioder asynkront | Maskinporten, scope `nav:dagpenger:afpprivat.read` |
| `POST /perioder` | Rettighetsperioder for interne | Entra ID |
| `POST /beregninger` | Beregnet brutto beløp fra Arena og dp-sak | Entra ID |
| `POST /meldekort` | Meldekort | Entra ID |
| `POST /dagpengestatus` | Om en person er i ny løsning (brukes av Arena) | Entra ID |

Interne kall må komme fra en applikasjon som er lagt til i `accessPolicy.inbound` i [`.nais/vars-dev.yaml`](.nais/vars-dev.yaml) eller `vars-prod.yaml`, med rollene applikasjonen trenger (`rettighetsperioder`, `beregninger`, `meldekort` eller `dagpengestatus`). Ny konsument må legges til der.

Eksterne kall via Maskinporten krever at organisasjonen er registrert som konsument av scopet. Per i dag er det Fellesordningen (AFP privat).

`POST /periode` oppretter en ressurs og svarer `201` med en URL. Klienten kaller URL-en til ressursen har status `FERDIG` (eller `FEILET`). Ressurser ryddes bort etter en konfigurert levetid.

## Arkitektur

- **Inn:** Appen er koblet til Rapids & Rivers på Kafka og lagrer behandlingsresultater og søknader i Postgres (Cloud SQL).
- **Ut:** Appen svarer på kall ved å lese fra Postgres og slå opp i dp-proxy (Arena), dp-meldekortregister og dp-arena-meldeplikt-adapter. Når den mottar et behandlingsresultat, publiserer den en OBO-melding til Kafka-topicet `obo.ytelser-v1`.
- **Leader election:** Én pod rydder bort gamle ressurser.
- **Migreringer:** Flyway kjører ved oppstart.

## Miljøer

| Miljø | Intern URL | Ekstern URL |
|---|---|---|
| dev | `https://dp-datadeling.intern.dev.nav.no` | `https://dp-datadeling.ekstern.dev.nav.no` |
| prod | | `https://www.nav.no/dagpenger/datadeling` |

Appen eksponerer `/isalive`, `/isready` og `/metrics` (Prometheus). Logger går til Loki og Elastic, og OpenTelemetry-instrumentering er slått på for Java. Se `.nais/nais.yaml` for detaljer.

## Utvikling

Du trenger Java 25 og Docker. Testene i `datadeling-api` starter Postgres med Testcontainers.

Repoet har ingen oppsett for å kjøre hele appen lokalt (Kafka, Postgres og eksterne tjenester må være tilgjengelige). Verifiser endringer med testene.

### Bygging og kodeformat

- Bygg og kjør alle sjekker: `./gradlew build`
- Kjør bare testene: `./gradlew test`
- Formater koden: `./gradlew ktlintFormat`

Bygging formaterer ikke koden. `./gradlew build` feiler på uformatert kode ([ktlint](https://ktlint.github.io/)), så kjør `ktlintFormat` før commit. I IntelliJ kan du installere ktlint-pluginen for å formatere automatisk.

Mer om konvensjoner og CI for utviklere og kodeassistenter står i [AGENTS.md](AGENTS.md).

### Moduler

Hvert subdomene har en egen modul.

- `openapi`: OpenAPI-spesifikasjonen og genererte DTO-er
- `soknad`: domenemodeller og logikk for søknader om dagpenger
- `behandling`: logikk for behandling og vedtak
- `meldekort`: domenemodeller og logikk for meldekort
- `datadeling-api`: REST-API, sikkerhet, migreringsskript og hovedapplikasjonen
- `ktor-client`: felles Ktor HTTP-klient
- `dato`: datohjelpere for tester (`1.januar(2020)`)

## Henvendelser

Opprett et issue her på GitHub. Nav-ansatte kan også bruke Slack-kanalen #team-dagpenger-data.
