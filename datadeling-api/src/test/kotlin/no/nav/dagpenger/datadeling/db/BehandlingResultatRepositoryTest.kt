package no.nav.dagpenger.datadeling.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.meldekortBeregning_v1
import no.nav.dagpenger.datadeling.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingResultatRepositoryTest {
    private val repository = BehandlingResultatRepositoryPostgresql()
    private val tolker = BehandlingResultatRepositoryMedTolker(repository)

    @Test
    fun `skal lagre og hente behandlingresultat`() {
        withMigratedDb {
            val behandlingId = UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
            val ident = "16261111906"
            val sakId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            val opprettetTidspunkt = LocalDateTime.of(2025, 11, 3, 12, 13, 40, 100308000)

            repository.lagre(
                ident = ident,
                behandlingId = behandlingId,
                basertPåId = null,
                sakId = sakId,
                json = innvilgelse_v1,
                opprettetTidspunkt = opprettetTidspunkt,
            )

            val hentet = tolker.hent(ident)
            hentet.size shouldBe 1
            hentet.first().behandlingId shouldBe behandlingId
        }
    }

    @Test
    fun `gir tom liste hvis ident ikke finnes`() {
        withMigratedDb {
            val ident = "16261111906"
            val hentet = repository.hent(ident)
            hentet.size shouldBe 0
        }
    }

    @Test
    fun `skal oppdater behandlingresultat hvis basertPå peker på en annen behandlingId`() {
        withMigratedDb {
            val behandlingId = UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
            val ident = "17373649758"
            val sakId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            val opprettetTidspunkt = LocalDateTime.of(2025, 11, 3, 12, 13, 40, 100308000)

            repository.lagre(
                ident = ident,
                behandlingId = behandlingId,
                basertPåId = null,
                sakId = sakId,
                json = innvilgelse_v1,
                opprettetTidspunkt = opprettetTidspunkt,
            )

            val hentet = tolker.hent(ident)
            hentet.size shouldBe 1
            hentet.first().behandlingId shouldBe behandlingId
            val behandlingId2 = UUID.fromString("019b4a53-1d46-7c36-ba6c-e4fb8435a4a1")
            val opprettetTidspunkt2 = LocalDateTime.of(2025, 11, 3, 12, 13, 40, 100308000)

            repository.lagre(
                ident = ident,
                behandlingId = behandlingId2,
                basertPåId = behandlingId,
                sakId = sakId,
                json = meldekortBeregning_v1,
                opprettetTidspunkt = opprettetTidspunkt2,
            )

            val hentBasertPå = tolker.hent(ident)
            hentBasertPå.size shouldBe 1
            hentBasertPå.first().behandlingId shouldBe behandlingId2
        }
    }

    @Test
    fun `skal lagre nytt behandlingresultat hvis basertPå mangler`() {
        withMigratedDb {
            val behandlingId = UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
            val ident = "17373649758"
            val sakId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            val opprettetTidspunkt = LocalDateTime.of(2025, 10, 1, 11, 11, 11, 100308000)

            repository.lagre(
                ident = ident,
                behandlingId = behandlingId,
                basertPåId = null,
                sakId = sakId,
                json = innvilgelse_v1,
                opprettetTidspunkt = opprettetTidspunkt,
            )

            val hentet = tolker.hent(ident)
            hentet.size shouldBe 1
            hentet.first().behandlingId shouldBe behandlingId

            val behandlingId2 = UUID.fromString("019b4a53-1d46-7c36-ba6c-e4fb8435a4a1")
            val opprettetTidspunkt2 = LocalDateTime.of(2025, 11, 3, 12, 13, 40, 100308000)

            repository.lagre(
                ident = ident,
                behandlingId = behandlingId2,
                basertPåId = null,
                sakId = sakId,
                json = meldekortBeregning_v1,
                opprettetTidspunkt = opprettetTidspunkt2,
            )

            val hentet2 = tolker.hent(ident).sortedBy { it.behandlingId }
            hentet2.size shouldBe 2
            hentet2.first().let { behandling ->
                behandling.ident shouldBe ident
                behandling.behandlingId shouldBe behandlingId
            }
            hentet2.last().let { behandling ->
                behandling.ident shouldBe ident
                behandling.behandlingId shouldBe behandlingId2
            }
        }
    }
}
