# Versjonshåndtering av BehandlingResultat tolkere

## Oversikt

Dette dokumentet beskriver strategien for versjonering av tolkere som mapper JSON fra `dp-behandling` til domeneobjekter.

## Prinsipper

### Command-Query Separation (CQS)
- **Command (Skriv)**: `BehandlingResultatMottak` lagrer rådata JSON uten tolkning
- **Query (Les)**: Tolkere mapper JSON til `BehandlingResultat` interface ved lesing

### Versjonering
- Hver endring i kontrakten fra `dp-behandling` får en ny tolker (V1, V2, etc.)
- Gamle tolkere beholdes for å kunne lese historisk data
- Factory-pattern velger riktig tolker basert på metadata i JSON

## Implementasjon

### 1. BehandlingResultatTolkerFactory
Factory som velger riktig tolker basert på:
1. **Tidsstempel** (`@opprettet` felt) - sammenlign med cutover-dato
2. **Eksplisitt versjonsfelt** (`kontraktVersjon`) - hvis `dp-behandling` legger det til
3. **Fallback** - bruk nyeste tolker som default

Implementert som `fun interface` med en standard-implementasjon `standardTolkerFactory`.

### 2. Lage ny tolker

Når kontrakten i `dp-behandling` endres:

```kotlin
// 1. Opprett ny tolker
data class BehandlingResultatV2Tolker(
    val jsonNode: JsonNode,
) : BehandlingResultat {
    // Implementer mapping for ny kontrakt
}

// 2. Oppdater enum i BehandlingResultatTolkerFactory.kt
private enum class TolkerVersjon(
    val gyldigTil: LocalDateTime,
) {
    V1(LocalDateTime.of(2025, 6, 1, 0, 0)), // Cutover-dato
    V2(LocalDateTime.MAX),
}

// 3. Utvid when-statement i standardTolkerFactory
val standardTolkerFactory = BehandlingResultatTolkerFactory { json ->
    val versjon = utledVersjon(json)
    when (versjon) {
        TolkerVersjon.V1 -> BehandlingResultatV1Tolker(json)
        TolkerVersjon.V2 -> BehandlingResultatV2Tolker(json)
    }
}
```

### 3. Testing

Hver tolker har sin egen test med testdata:
```
src/test/resources/testdata/
  ├── behandlingresultat-v1.json
  ├── behandlingresultat-v2.json
  └── ...
```

### 4. Bruk i repository

```kotlin
class BehandlingResultatRepositoryImpl(
    private val tolkerFactory: BehandlingResultatTolkerFactory = standardTolkerFactory
) : BehandlingResultatRepository {
    
    override fun hent(ident: String): List<BehandlingResultat> {
        return database.hentJson(ident).map { jsonString ->
            val json = objectMapper.readTree(jsonString)
            tolkerFactory.hentTolker(json)
        }
    }
}

// Eller bruk direkte hvis du ikke trenger custom factory:
override fun hent(ident: String): List<BehandlingResultat> {
    return database.hentJson(ident).map { jsonString ->
        val json = objectMapper.readTree(jsonString)
        standardTolkerFactory.hentTolker(json)
    }
}

// For testing kan du enkelt lage mock:
val testFactory = BehandlingResultatTolkerFactory { json ->
    BehandlingResultatV1Tolker(json)
}
```

## Når kan en tolker slettes?

En tolker kan slettes når:
1. All data i databasen er nyere enn tolkerens `gyldigTil` dato
2. Ingen konsumenter trenger historisk reprosessering
3. Minst 2 år har gått siden cutover (NAV retention policy)

## Metrikker

Legg til metrics for å overvåke hvilke tolkere som brukes:
```kotlin
private fun utledVersjon(json: JsonNode, meterRegistry: MeterRegistry?): TolkerVersjon {
    val versjon = // ... logikk
    meterRegistry?.counter(
        "behandling_resultat_tolker_bruk",
        "versjon", versjon.name
    )?.increment()
    return versjon
}
```

## Fallgruver å unngå

- **OpplysningTypeId endring**: Hvis UUID-er endres, legg til mapping-tabell
- **Nye enum-verdier**: Bruk `UNKNOWN` som fallback
- **Nullable fields**: Ha sane defaults for felter som mangler i gamle data
- **Breaking changes**: Test ny tolker mot alle eksisterende testdata-filer

## Fordeler med fun interface

- **Enklere testing**: Lett å lage mock med lambda
- **Mindre boilerplate**: Ingen ekstra klasser å lage
- **Fleksibilitet**: Kan byttes ut runtime hvis nødvendig
- **Lesbarhet**: Mindre kode å vedlikeholde
