package no.nav.dagpenger.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.endring_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class BehandlingResultatMottakTest {
    private val testRapid = TestRapid()
    private val behandlingResultatRepositoryPostgresql = mockk<BehandlingResultatRepository>()
    private val producerMock = mockk<KafkaProducer<String, String>>()

    init {
        BehandlingResultatMottak(
            rapidsConnection = testRapid,
            behandlingResultatRepository = behandlingResultatRepositoryPostgresql,
            konsument =
                SammensattKonsument(
                    OboDagpengerStatusKonsument(
                        producer = producerMock,
                        topic = "obo-topic",
                        objectMapper = objectMapper,
                    ),
                ),
        )
    }

    @BeforeEach
    internal fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta behandlingresultat og sender opprettet-signal til arbeidsrettet oppfølging`() {
        val ident = slot<String>()
        val behandlingId = slot<UUID>()
        val basertPåId = slot<UUID?>()
        val sakId = slot<UUID>()
        val json = slot<String>()
        val opprettetTidspunkt = slot<LocalDateTime>()
        val utgåendeMelding = slot<ProducerRecord<String, String>>()

        every {
            behandlingResultatRepositoryPostgresql.lagre(
                ident = capture(ident),
                behandlingId = capture(behandlingId),
                basertPåId = captureNullable(basertPåId),
                sakId = capture(sakId),
                json = capture(json),
                opprettetTidspunkt = capture(opprettetTidspunkt),
            )
        } returns Unit

        every {
            producerMock.send(capture(utgåendeMelding))
        } returns mockk()

        testRapid.sendTestMessage(innvilgelse_v1)

        verify {
            behandlingResultatRepositoryPostgresql.lagre(any(), any(), any(), any(), any(), any())
        }

        behandlingId.captured shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        basertPåId.captured shouldBe null
        ident.captured shouldBe "17373649758"
        sakId.captured shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        opprettetTidspunkt.captured shouldBe LocalDateTime.parse("2025-12-23T09:26:50.618236")

        with(utgåendeMelding.captured) {
            key() shouldBe "17373649758"
            topic() shouldBe "obo-topic"
            val jsonmelding = objectMapper.readTree(value())
            jsonmelding["personId"].asText() shouldBe "17373649758"
            jsonmelding["meldingstype"].asText() shouldBe "OPPRETT"
            jsonmelding["ytelsestype"].asText() shouldBe "DAGPENGER"
            jsonmelding["kildesystem"].asText() shouldBe "DPSAK"
        }
    }

    @Test
    fun `vi kan motta behandlingresultat og sender oppdater-signal til arbeidsrettet oppfølging`() {
        val ident = slot<String>()
        val behandlingId = slot<UUID>()
        val basertPåId = slot<UUID?>()
        val sakId = slot<UUID>()
        val json = slot<String>()
        val opprettetTidspunkt = slot<LocalDateTime>()
        val utgåendeMelding = slot<ProducerRecord<String, String>>()

        every {
            behandlingResultatRepositoryPostgresql.lagre(
                ident = capture(ident),
                behandlingId = capture(behandlingId),
                basertPåId = captureNullable(basertPåId),
                sakId = capture(sakId),
                json = capture(json),
                opprettetTidspunkt = capture(opprettetTidspunkt),
            )
        } returns Unit

        every {
            producerMock.send(capture(utgåendeMelding))
        } returns mockk()

        testRapid.sendTestMessage(endring_v1)

        verify {
            behandlingResultatRepositoryPostgresql.lagre(any(), any(), any(), any(), any(), any())
        }

        behandlingId.captured shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        basertPåId.captured shouldBe null
        ident.captured shouldBe "17373649758"
        sakId.captured shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        opprettetTidspunkt.captured shouldBe LocalDateTime.parse("2025-12-23T09:26:50.618236")

        with(utgåendeMelding.captured) {
            key() shouldBe "17373649758"
            topic() shouldBe "obo-topic"
            val jsonmelding = objectMapper.readTree(value())
            jsonmelding["personId"].asText() shouldBe "17373649758"
            jsonmelding["meldingstype"].asText() shouldBe "OPPDATER"
            jsonmelding["ytelsestype"].asText() shouldBe "DAGPENGER"
            jsonmelding["kildesystem"].asText() shouldBe "DPSAK"
        }
    }

    @Test
    fun `vi mapper førteTil til riktig meldingstype`() {
        DagpengerHendelse.fraFørteTil("123", "Innvilgelse").meldingstype shouldBe DagpengerHendelse.Meldingstype.OPPRETT
        DagpengerHendelse.fraFørteTil("123", "Revurdering").meldingstype shouldBe DagpengerHendelse.Meldingstype.OPPDATER
    }

    @Test
    fun `vi kan varsle flere konsumenter via abstraksjonen`() {
        val konsument1 = mockk<DagpengerStatusKonsument>(relaxed = true)
        val konsument2 = mockk<DagpengerStatusKonsument>(relaxed = true)
        val varsler = SammensattKonsument(konsument1, konsument2)
        val varsel = DagpengerHendelse(ident = "123", meldingstype = DagpengerHendelse.Meldingstype.OPPRETT)

        varsler.varsle(varsel)
        verify(exactly = 1) { konsument1.varsle(varsel) }
        verify(exactly = 1) { konsument2.varsle(varsel) }
    }
}
