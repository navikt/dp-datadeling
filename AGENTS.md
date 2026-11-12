# AGENTS.md

REST-API for deling av dagpengedata til interne og eksterne aktører. Kotlin, Ktor, Postgres og Rapids & Rivers, bygget med Gradle og kjørt på Nais.

## Forutsetninger

- Java 25 (Temurin). Gradle-toolchain er satt til 25.
- Docker kjører lokalt. Testene i `datadeling-api` starter Postgres 18.4 med Testcontainers.
- Bruk alltid `./gradlew`, ikke en global Gradle.

## Kommandoer

| Oppgave | Kommando |
|---------|----------|
| Alle tester | `./gradlew test` |
| Tester i én modul | `./gradlew :behandling:test` |
| Full bygging og sjekk | `./gradlew build` |
| Sjekk kodeformat | `./gradlew ktlintCheck` |
| Formater kode | `./gradlew ktlintFormat` |

Bygging formaterer ikke koden. Kjør `ktlintFormat` før commit. `ktlintCheck` er en del av `build` og feiler på uformatert kode.

## Moduler

- `openapi`: OpenAPI-spesifikasjonen (`src/main/resources/datadeling-api.yaml`). DTO-ene med suffikset `DTO` genereres med Fabrikt til `build/generated`. Endre YAML-en, ikke de genererte filene.
- `soknad`, `behandling`, `meldekort`: domenemodeller og logikk per subdomene.
- `datadeling-api`: REST-API, sikkerhet, Flyway-migreringer og hovedapplikasjonen (`App.kt`, `ApplicationBuilder.kt`, `Config.kt`).
- `ktor-client`: felles HTTP-klient med retry.
- `dato`: hjelpefunksjoner for datoer i tester (`1.januar(2020)`).

## Konvensjoner

- Avhengighetsversjoner kommer fra `dp-version-catalog` (se `settings.gradle.kts`). Legg nye versjoner der, ikke hardkodet i modulene, når det er mulig.
- Databasemigreringer ligger i `datadeling-api` og er versjonerte. Aldri endre en migrering som allerede er kjørt.
- Tester bruker `kotlin.test` og JUnit 5, med Kotest-matchers og MockK. Databasetester bruker `Postgres.withMigratedDb { ... }` fra testkoden i `datadeling-api`.
- Tester som kaller reelle miljøer er markert `@Disabled`. Ikke skru dem på.
- Konfigurasjon leses med Konfig i `Config.kt` (miljøvariabler overstyrer standardverdier, `prod-gcp` har egne verdier). Funksjonsflagget `FEATURE_ARENA_PERIODER` leses i `BeregningerService.kt`. Ikke hardkod hemmeligheter.

## Personvern og sikkerhet

- Aldri logg fødselsnummer, navn eller adresse. Logg heller IDer som ressurs-UUID.
- Ikke bruk ekte personidenter i testdata.
- Endringer i autentisering, `accessPolicy` i `.nais/nais.yaml` eller OpenAPI-kontrakten påvirker eksterne konsumenter. Spør før du endrer dem.

## CI/CD

- `deploy.yaml` bygger og publiserer image ved push. Deploy til dev og prod skjer bare fra `main`.
- `test.yaml` kjører `./gradlew test` for dependency-PR-er og i merge queue.
- `copilot-sync.yml` synker filene i `.github/instructions`, `.github/agents`, `.github/skills` og `.github/prompts` fra `navikt/copilot`. Lokale endringer der blir overskrevet. Repo-spesifikke instruksjoner ligger i `AGENTS.md` og `.github/instructions/dp-datadeling.instructions.md`.
