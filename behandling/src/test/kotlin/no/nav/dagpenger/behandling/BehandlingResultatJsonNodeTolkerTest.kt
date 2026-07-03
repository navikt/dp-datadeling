package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.meldekortBeregning_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.stans_v1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class BehandlingResultatJsonNodeTolkerTest {
    @Test
    fun `tolke behandlingresultat v1`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(innvilgelse_v1)
        val tolker = BehandlingResultatJsonNodeTolker.fra(behandlingResultat)

        tolker.behandlingId shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        tolker.ident shouldBe "17373649758"

        tolker.rettighetsperioder.size shouldBe 1
        tolker.rettighetsperioder.first().fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        tolker.rettighetsperioder.first().tilOgMed shouldBe null
        tolker.rettighetsperioder.first().harRett shouldBe true

        tolker.rettighetstyper.size shouldBe 1
        tolker.rettighetstyper.first().type shouldBe Rettighetstype.ORDINÆR
    }

    @Test
    fun `tar med alle rettighetsperioder`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(stans_v1)
        val tolker = BehandlingResultatJsonNodeTolker.fra(behandlingResultat)

        tolker.behandlingId shouldBe UUID.fromString("019b4a54-0d82-7a74-b0aa-a19d160016f8")
        tolker.ident shouldBe "10399847102"

        tolker.rettighetsperioder.size shouldBe 2
        tolker.rettighetsperioder.first { it.harRett }.fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        tolker.rettighetsperioder.first { it.harRett }.tilOgMed shouldBe LocalDate.of(2018, 7, 21)
        tolker.rettighetsperioder.first { !it.harRett }.fraOgMed shouldBe LocalDate.of(2018, 7, 22)

        tolker.rettighetstyper.size shouldBe 1
        tolker.rettighetstyper.first().type shouldBe Rettighetstype.ORDINÆR
    }

    @Test
    fun `har utbetalinger`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(meldekortBeregning_v1)
        val tolker = BehandlingResultatJsonNodeTolker.fra(behandlingResultat)

        tolker.beregninger.size shouldBe 11
        tolker.beregninger[0].dato shouldBe LocalDate.of(2018, 6, 21)
        tolker.beregninger[0].sats shouldBe 1259
        tolker.beregninger[0].utbetaling shouldBe 719
        tolker.beregninger[0].gjenståendeDager shouldBe 519

        tolker.beregninger[10].dato shouldBe LocalDate.of(2018, 7, 1)
        tolker.beregninger[10].sats shouldBe 1259
        tolker.beregninger[10].utbetaling shouldBe 0
        tolker.beregninger[10].gjenståendeDager shouldBe 513

        tolker.beregninger.sumOf { it.utbetaling } shouldBe 5036
    }

    @Test
    fun `tolererer gammel json uten dagpengeType`() {
        val gammelJson: JsonNode =
            objectMapper.readTree(
                // language=JSON
                """
                {
                  "behandlingId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                  "behandletHendelse": {
                    "datatype": "UUID",
                    "id": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                    "type": "Søknad",
                    "skjedde": "2026-03-15"
                  },
                  "behandlingskjedeId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                  "automatisk": true,
                  "regelverk": "Dagpenger",
                  "ident": "12345678901",
                  "rettighetsperioder": [
                    {
                      "fraOgMed": "2026-01-01",
                      "harRett": false
                    }
                  ],
                  "opprettet": "2026-03-15T10:00:00",
                  "sistEndret": "2026-03-15T10:00:00",
                  "opplysninger": [
                    {
                      "opplysningTypeId": "01992956-e349-76b1-8f68-c9d481df3a32",
                      "navn": "Antall dager som gjenstår",
                      "datatype": "heltall",
                      "perioder": [
                        {
                          "id": "019b4a53-1d7d-70a4-876f-57563365603c",
                          "opprettet": "2025-12-23T09:28:39.677527",
                          "opprinnelse": "Ny",
                          "gyldigFraOgMed": "2018-06-18",
                          "gyldigTilOgMed": "2018-06-18",
                          "verdi": {
                            "verdi": 520,
                            "enhet": "dager",
                            "datatype": "heltall"
                          }
                        }
                      ]
                    }
                  ],
                  "utbetalinger": [
                    {
                      "dato": "2026-05-01",
                      "sats": 4024,
                      "utbetaling": 4024,
                      "opprinnelse": "Ny",
                      "meldeperiode": "Ferietillegg-2025"
                    }
                  ],
                  "behandletAv": [],
                  "førteTil": "Avslag"
                }
                """.trimIndent(),
            )

        val tolker = BehandlingResultatJsonNodeTolker.fra(gammelJson)
        shouldNotThrowAny { tolker.beregninger }
        // Skal bruke siste tilgjengelige gjenstående-dager-periode (2018-06-18 → 520),
        // selv om utbetalingen er på 2026-05-01
        tolker.beregninger.single().gjenståendeDager shouldBe 520
    }

    @Test
    fun `bruker siste tilgjengelige gjenstående-dager-periode ved manglende eksakt match`() {
        val json: JsonNode =
            objectMapper.readTree(
                // language=JSON
                """
                {
                  "behandlingId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                  "behandletHendelse": {"datatype": "UUID", "id": "019b4a51-6ef8-7714-8f5f-924a23137d03", "type": "Søknad", "skjedde": "2026-03-15"},
                  "behandlingskjedeId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                  "automatisk": true,
                  "regelverk": "Dagpenger",
                  "ident": "12345678901",
                  "rettighetsperioder": [{"fraOgMed": "2026-01-01", "harRett": true}],
                  "opprettet": "2026-03-15T10:00:00",
                  "sistEndret": "2026-03-15T10:00:00",
                  "opplysninger": [
                    {
                      "opplysningTypeId": "01992956-e349-76b1-8f68-c9d481df3a32",
                      "navn": "Antall dager som gjenstår",
                      "datatype": "heltall",
                      "perioder": [
                        {
                          "id": "00000000-0000-0000-0000-000000000001",
                          "gyldigFraOgMed": "2026-06-01",
                          "verdi": {"verdi": 89, "datatype": "heltall"}
                        },
                        {
                          "id": "00000000-0000-0000-0000-000000000002",
                          "gyldigFraOgMed": "2026-06-02",
                          "verdi": {"verdi": 90, "datatype": "heltall"}
                        }
                      ]
                    }
                  ],
                  "utbetalinger": [
                    {"dato": "2026-06-01", "sats": 1000, "utbetaling": 1000},
                    {"dato": "2026-06-02", "sats": 1000, "utbetaling": 1000},
                    {"dato": "2026-06-03", "sats": 1000, "utbetaling": 0}
                  ],
                  "behandletAv": [],
                  "førteTil": "Endring"
                }
                """.trimIndent(),
            )

        val tolker = BehandlingResultatJsonNodeTolker.fra(json)
        // 2026-06-15 har ingen eksakt match — skal velge siste periode før dato: 2026-06-10 → 90 dager
        with(tolker.beregninger[0]) {
            dato.toString() shouldBe "2026-06-01"
            gjenståendeDager shouldBe 89
            utbetaling shouldBe 1000
        }
        with(tolker.beregninger[1]) {
            dato.toString() shouldBe "2026-06-02"
            gjenståendeDager shouldBe 90
            utbetaling shouldBe 1000
        }
        with(tolker.beregninger[2]) {
            dato.toString() shouldBe "2026-06-03"
            gjenståendeDager shouldBe 90
            utbetaling shouldBe 0
        }
    }

    @Test
    fun `tolererer manglende utbetalinger-felt`() {
        val jsonUtenUtbetalinger: JsonNode =
            objectMapper.readTree(
                // language=JSON
                """
                {
                    "behandlingId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                    "behandletHendelse": {"datatype": "UUID", "id": "019b4a51-6ef8-7714-8f5f-924a23137d03", "type": "Søknad", "skjedde": "2026-03-15"},
                    "behandlingskjedeId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                    "automatisk": true,
                    "regelverk": "Dagpenger",
                    "ident": "12345678901",
                    "rettighetsperioder": [{"fraOgMed": "2026-01-01", "harRett": false}],
                    "opprettet": "2026-03-15T10:00:00",
                    "sistEndret": "2026-03-15T10:00:00",
                    "opplysninger": [],
                    "behandletAv": [],
                    "førteTil": "Avslag"
                }
                """.trimIndent(),
            )

        val tolker = BehandlingResultatJsonNodeTolker.fra(jsonUtenUtbetalinger)
        tolker.beregninger shouldBe emptyList()
    }
}
