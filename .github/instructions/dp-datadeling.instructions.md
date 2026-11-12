---
applyTo: "**/src/test/**/*.kt"
---

# Tester i dp-datadeling

Testfilene her heter `*Test.kt` og ligger under `src/test`. Dette er en repo-lokal instruksjon fordi de synkede testinstruksjonene bare matcher `*.test.kt`.

- Følg `AGENTS.md` for kommandoer og forutsetninger.
- Bruk `kotlin.test` med JUnit 5, Kotest-matchers og MockK.
- Databasetester bruker `Postgres.withMigratedDb { ... }` og Testcontainers. Docker må kjøre.
- API-tester bruker `TestApplication.testEndepunkter` og tokens fra MockOAuth2Server.
- Bruk syntetiske personidenter. Ikke bruk ekte fødselsnumre.
- Én test per atferd, med navn som beskriver atferden (backtick-navn).
