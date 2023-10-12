# dp-datadeling

Rest-API for deling av dagpengerelaterte data til interne og eksterne aktører

## Bygging

For å få tilgang til dp-kontrakter, må man opprette en fil som heter gradle.properties i GRADLE_USER_HOME (som by
default er %user_home%/.gradle)  
Filen må inneholde:

```
githubUser=x-access-token
githubPassword=%YOUR_GITHUB_TOKEN%
```

Bygg ved å kjøre `./gradlew clean build`. Dette vil også kjøre testene.

## Kjør appen lokalt

Start appen ved å kjøre main-funksjonen tli `LocalApp`

## Swagger

Swagger UI: http://localhost:8080/internal/swagger-ui/index.html

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles ved å opprette et issue her på Github.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-dagpenger-dev.