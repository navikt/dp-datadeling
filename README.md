# dp-datadeling

Rest-API for deling av dagpengerelaterte data til interne og eksterne aktører.

## Bygging

Bygg ved å kjøre `./gradlew build`

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


