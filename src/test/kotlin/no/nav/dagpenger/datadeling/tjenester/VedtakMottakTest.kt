package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.datadeling.db.VedtakRepository
import no.nav.dagpenger.datadeling.model.Vedtak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VedtakMottakTest {
    private val testRapid = TestRapid()
    private val vedtakRepository = mockk<VedtakRepository>()

    init {
        every { vedtakRepository.lagreVedtak(any(), any(), any(), any(), any(), any(), any()) } returns 1

        VedtakMottak(testRapid, vedtakRepository)
    }

    @BeforeEach
    internal fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta vedtak (v1)`() {
        testRapid.sendTestMessage(vedtakJsonV1)

        verify {
            vedtakRepository.lagreVedtak(
                eq("01020312345"),
                eq("29501880"),
                eq("123"),
                eq(Vedtak.Status.INNVILGET),
                eq(LocalDateTime.parse("2020-04-07T14:31:08.840468")),
                eq(LocalDateTime.parse("2018-03-05T00:00:00")),
                null,
            )
        }
    }

    @Test
    fun `vi kan motta vedtak (v2)`() {
        testRapid.sendTestMessage(vedtakJsonV2)

        verify {
            vedtakRepository.lagreVedtak(
                eq("01020312345"),
                eq("29501880"),
                eq("123"),
                eq(Vedtak.Status.INNVILGET),
                eq(LocalDateTime.parse("2021-11-12T08:31:33.092337")),
                eq(LocalDateTime.parse("2018-03-05T00:00:00")),
                eq(LocalDateTime.parse("2022-01-06T00:00:00")),
            )
        }
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
