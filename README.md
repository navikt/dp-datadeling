# dp-datadeling

DP datadelingskomponent

## Bygging

For å få tilgang til dp-kontrakter, må man opprette en fil som heter gradle.properties i GRADLE_USER_HOME (som by
default er %user_home%/.gradle)  
Filen må inneholde:

```
githubUser=x-access-token
githubPassword=%YOUR_GITHUB_TOKEN%
```

Bygg ved å kjøre `./gradlew clean build`. Dette vil også kjøre testene.

## Lokalkjøring

Start appen `./gradlew runServerTest`

## SwaggerUI

Swagger UI: http://localhost:8080/internal/swagger-ui/index.html

## Kontaktinfo

For NAV-interne kan henvendelser om appen rettes til #team-dagpenger på slack. Ellers kan man opprette et issue her på
github.
