# dp-datadeling

Rest-API for deling av dagpengerelaterte data til interne og eksterne aktører.

## Bygging

For å få tilgang til [dp-kontrakter](https://github.com/navikt/dp-kontrakter), må man opprette en fil som heter gradle.properties i GRADLE_USER_HOME (som by
default er %user_home%/.gradle)  
Filen må inneholde:

```
githubUser=x-access-token
githubPassword=%YOUR_GITHUB_TOKEN%
```

Bygg ved å kjøre `./gradlew build`

## Dokumentasjon 

Swaggerdoc blir publisert til: https://navikt.github.io/dp-datadeling/openapi.html

Perioder med dagpengerettigheter hentes foreløig kun Arena, via:
[dp-proxy](https://github.com/navikt/dp-proxy/blob/main/proxy/src/main/kotlin/no/nav/dagpenger/proxy/feature/ArenaDagpengerPerioder.kt)
Når ny dagpengeløsning er på lufta, skal vedtaksinformasjon hentes både fra Arena og fra ny løsning.

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles ved å opprette et issue her på Github.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-arbeid-dev.