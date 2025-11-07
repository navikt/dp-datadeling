# dp-datadeling

Rest-API for deling av dagpengerelaterte data til interne og eksterne aktører.

## Bygging

Bygg ved å kjøre `./gradlew build`


### For utviklere i Dagpenger

Hvert subdomene har egen modul for å gjøre det enklere å finne frem i koden.

- `openapi` - Inneholder OpenAPI-spesifikasjonen og DTO-er
- `soknad` - Inneholder domenemodeller og logikk knyttet til søknader om dagpenger
- `behandling` - Inneholder logikk knyttet til behandling om dagpenger og vedtak
- `meldekort` - Inneholder domenemodeller og logikk knyttet til meldekort om dagpenger
- `datadeling-api` - Inneholder REST-API for datadeling, konfigurering av sikkerhet, migreringsskripter og hovedapplikasjonen


Andre moduler:
- `ktor-client` - Default Ktor HTTP-klient 
- `dato` - Felles dato brukt for testing (kan lage 1.januar(2020) osv)

### Linting

Prosjektet bruker [ktlint](https://ktlint.github.io/) for å sikre enhetlig kodeformat. Hvis du bruker IntelliJ, anbefales det å installere ktlint-plugin for automatisk formatering av koden.

## Dokumentasjon 

Swaggerdoc blir publisert til: https://navikt.github.io/dp-datadeling/openapi.html

Perioder med dagpengerettigheter hentes foreløig kun Arena, via:
[dp-proxy](https://github.com/navikt/dp-proxy/blob/main/proxy/src/main/kotlin/no/nav/dagpenger/proxy/feature/ArenaDagpengerPerioder.kt)
Når ny dagpengeløsning er på lufta, skal vedtaksinformasjon hentes både fra Arena og fra ny løsning.





## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles ved å opprette et issue her på Github.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-arbeid-dev.


