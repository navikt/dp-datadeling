package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArenaVedtakMottakTest {
    private val testRapid = TestRapid()

    init {
        ArenaVedtakMottak(testRapid)
    }

    @BeforeEach
    internal fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta vedtak (v1)`() {
        testRapid.sendTestMessage(vedtakJsonV1)

        // TODO: Test når vi begynner å sende "eksterne"-hendelser videre
    }

    @Test
    fun `vi kan motta vedtak (v2)`() {
        testRapid.sendTestMessage(vedtakJsonV2)

        // TODO: Test når vi begynner å sende "eksterne"-hendelser videre
    }
}

//language=JSON
private val vedtakJsonV1 =
    """
    {
      "table": "SIAMO.VEDTAK",
      "op_type": "I",
      "op_ts": "2020-04-07 14:31:08.840468",
      "current_ts": "2020-04-07T14:53:03.656001",
      "pos": "00000000000000013022",
      "tokens": {
        "FODSELSNR": "01020312345"
      },
      "after": {
        "VEDTAK_ID": 29501880,
        "SAK_ID": 123,
        "VEDTAKSTATUSKODE": "IVERK",
        "VEDTAKTYPEKODE": "O",
        "UTFALLKODE": "JA",
        "RETTIGHETKODE": "DAGO",
        "PERSON_ID": 4124685,
        "FRA_DATO": "2018-03-05 00:00:00",
        "TIL_DATO": null
      }
    }
    """.trimIndent()

//language=JSON
private val vedtakJsonV2 =
    """
    {
        "table": "SIAMO.VEDTAK",
        "op_type": "I",
        "op_ts": "2021-11-12 08:31:33.092337",
        "current_ts": "2021-11-12 08:57:55.082000",
        "pos": "00000000000000010892",
        "FODSELSNR": "01020312345",
        "after": {
            "VEDTAK_ID": 29501880,
            "SAK_ID": 123,
            "VEDTAKSTATUSKODE": "IVERK",
            "VEDTAKTYPEKODE": "O",
            "UTFALLKODE": "JA",
            "RETTIGHETKODE": "DAGO",
            "PERSON_ID": 4124685,
            "FRA_DATO": "2018-03-05 00:00:00",
            "TIL_DATO": "2022-01-06 00:00:00"
        }
    }
    """.trimIndent()
