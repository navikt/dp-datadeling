package no.nav.dagpenger.datadeling.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import no.nav.dagpenger.datadeling.Postgres.withMigratedDb
import no.nav.dagpenger.søknad.modell.Søknad
import no.nav.dagpenger.søknad.modell.Søknadsbehandling
import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SøknadBehandlingGrafTest {
    private val repository = SøknadsbehandlingRepositoryPostgresql()
    private val resultater = BehandlingResultatRepositoryPostgresql()
    private val graf = SøknadBehandlingGrafPostgresql(BehandlingResultatRepositoryMedTolker(resultater))
    private val ident = "01020312345"
    private val søknadId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val sakId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val behandlingId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val opprettet = LocalDateTime.of(2025, 11, 3, 12, 0)
    private val ferdig = opprettet.plusDays(2)

    private fun opprettet(tidspunkt: LocalDateTime = opprettet) = repository.lagreOpprettet(søknadId, behandlingId, sakId, ident, tidspunkt)

    private fun tilstand(
        tilstand: SøknadsbehandlingTilstand,
        tidspunkt: LocalDateTime = ferdig,
        utfall: String? = null,
    ) = repository.lagreTilstand(søknadId, behandlingId, sakId, ident, tilstand, tidspunkt, utfall)

    private val innsendt = opprettet.minusHours(1)

    private fun SøknadBehandlingGrafPostgresql.hent(ident: String) =
        søknaderForPerson(ident)
            .flatMap {
                it.søknadsbehandlinger
            }.utenSistEndret()

    private fun SøknadBehandlingGrafPostgresql.hent(sakId: UUID) = søknaderForSak(sakId).flatMap { it.søknadsbehandlinger }.utenSistEndret()

    private fun List<Søknadsbehandling>.utenSistEndret() = map { it.copy(sistEndret = null) }

    private fun innsending(
        id: String?,
        journalpostId: String = "jp-$id",
        kanal: Søknad.Kanal = Søknad.Kanal.Digital,
    ) = SøknadRepositoryPostgresql().lagre(
        ident,
        id,
        journalpostId,
        "NAV 04-01.03",
        Søknad.SøknadsType.NySøknad,
        kanal,
        innsendt,
    )

    @Test
    fun `innsending uten søknadsbehandling er Innsendt uten sak og vises ikke på sakId`() =
        withMigratedDb {
            innsending("$søknadId")

            graf.hent(ident) shouldBe
                listOf(Søknadsbehandling(søknadId, null, innsendt, SøknadsbehandlingTilstand.Innsendt))
            graf.hent(sakId) shouldBe emptyList()
        }

    @Test
    fun `innsending først gir innsendt fra soknad og tilstand fra søknadsbehandling`() =
        withMigratedDb {
            innsending("$søknadId")
            opprettet()
            tilstand(SøknadsbehandlingTilstand.Ferdig, utfall = "Innvilgelse")

            val forventet =
                Søknadsbehandling(søknadId, sakId, innsendt, SøknadsbehandlingTilstand.Ferdig, "Innvilgelse", behandlingId = behandlingId)
            graf.hent(ident) shouldBe listOf(forventet)
            graf.hent(sakId) shouldBe listOf(forventet)
        }

    @Test
    fun `opprettet uten soknad-rad bruker opprettet som innsendt, og innsending senere gir innsendt fra soknad`() =
        withMigratedDb {
            opprettet()
            graf.hent(sakId).single().innsendt shouldBe opprettet

            innsending("$søknadId")
            graf.hent(sakId).single().innsendt shouldBe innsendt
        }

    @Test
    fun `papirsøknader og ikke-UUID søknadId feiler ikke og utelates`() =
        withMigratedDb {
            innsending(null, "jp-papir", Søknad.Kanal.Papir)
            innsending("ikke-en-uuid")
            opprettet()

            graf.hent(ident).map { it.søknadId } shouldBe listOf(søknadId)
        }

    @Test
    fun `avbrutt søknad hentes med tilstand Avbrutt`() =
        withMigratedDb {
            innsending("$søknadId")
            tilstand(SøknadsbehandlingTilstand.Avbrutt)

            graf.hent(sakId).single().tilstand shouldBe SøknadsbehandlingTilstand.Avbrutt
        }

    private fun lagreResultat() = resultater.lagre(ident, behandlingId, null, sakId, innvilgelse_v1, opprettet)

    @Test
    fun `søknad kobles til behandlingsresultat via behandlingId, og omvendt`() =
        withMigratedDb {
            innsending("$søknadId")
            opprettet()
            lagreResultat()

            graf.søknadForBehandling(behandlingId)?.søknadId shouldBe søknadId
            graf.resultaterForSøknad(søknadId).single().opprettet shouldBe opprettet
        }

    @Test
    fun `søknad uten resultat gir tom resultatliste, og ukjent behandling gir ingen søknad`() =
        withMigratedDb {
            opprettet()

            graf.resultaterForSøknad(søknadId) shouldBe emptyList()
            graf.søknadForBehandling(UUID.randomUUID()) shouldBe null
        }

    @Test
    fun `søknader for sak grupperes per søknad`() =
        withMigratedDb {
            opprettet()

            graf.søknaderForSak(sakId).map { it.søknadId } shouldBe listOf(søknadId)
            graf
                .søknaderForPerson(ident)
                .single()
                .søknadsbehandlinger
                .single()
                .behandlingId shouldBe behandlingId
        }

    private val andreBehandlingId = UUID.fromString("44444444-4444-4444-4444-444444444444")
    private val andreSakId = UUID.fromString("55555555-5555-5555-5555-555555555555")

    @Test
    fun `to behandlinger for samme søknad i samme sak gir én søknad med to behandlinger`() =
        withMigratedDb {
            innsending("$søknadId")
            opprettet()
            repository.lagreOpprettet(søknadId, andreBehandlingId, sakId, ident, opprettet.plusDays(1))
            tilstand(SøknadsbehandlingTilstand.Avbrutt)

            val søknad = graf.søknaderForPerson(ident).single()
            søknad.søknadsbehandlinger.map { it.behandlingId to it.tilstand }.toSet() shouldBe
                setOf(
                    behandlingId to SøknadsbehandlingTilstand.Avbrutt,
                    andreBehandlingId to SøknadsbehandlingTilstand.UnderBehandling,
                )
            graf
                .søknaderForSak(sakId)
                .single()
                .søknadsbehandlinger.size shouldBe 2
            graf.søknadForBehandling(andreBehandlingId)?.søknadId shouldBe søknadId
        }

    @Test
    fun `behandlinger i ulike saker gir søknaden i hver sak`() =
        withMigratedDb {
            opprettet()
            repository.lagreOpprettet(søknadId, andreBehandlingId, andreSakId, ident, opprettet.plusDays(1))

            graf
                .søknaderForSak(sakId)
                .single()
                .søknadsbehandlinger
                .single()
                .behandlingId shouldBe behandlingId
            graf
                .søknaderForSak(andreSakId)
                .single()
                .søknadsbehandlinger
                .single()
                .behandlingId shouldBe andreBehandlingId
            graf
                .søknaderForPerson(ident)
                .single()
                .søknadsbehandlinger.size shouldBe 2
        }

    @Test
    fun `ferdig og avbrutt oppdaterer bare sin egen behandling`() =
        withMigratedDb {
            opprettet()
            repository.lagreOpprettet(søknadId, andreBehandlingId, sakId, ident, opprettet.plusDays(1))
            repository.lagreTilstand(søknadId, andreBehandlingId, sakId, ident, SøknadsbehandlingTilstand.Ferdig, ferdig, "Innvilgelse")

            val tilstander = graf.hent(sakId).associate { it.behandlingId to it.tilstand }
            tilstander[behandlingId] shouldBe SøknadsbehandlingTilstand.UnderBehandling
            tilstander[andreBehandlingId] shouldBe SøknadsbehandlingTilstand.Ferdig
        }

    @Test
    fun `ferdig lagrer utfall`() =
        withMigratedDb {
            opprettet()
            tilstand(SøknadsbehandlingTilstand.Ferdig, utfall = "Innvilgelse")

            graf.hent(sakId) shouldBe
                listOf(
                    Søknadsbehandling(
                        søknadId,
                        sakId,
                        opprettet,
                        SøknadsbehandlingTilstand.Ferdig,
                        "Innvilgelse",
                        behandlingId = behandlingId,
                    ),
                )
        }

    @Test
    fun `opprettet søknadsbehandling er under behandling og kan hentes på ident og sak`() =
        withMigratedDb {
            opprettet()

            val forventet =
                listOf(
                    Søknadsbehandling(søknadId, sakId, opprettet, SøknadsbehandlingTilstand.UnderBehandling, behandlingId = behandlingId),
                )
            graf.hent(ident) shouldBe forventet
            graf.hent(sakId) shouldBe forventet
        }

    @Test
    fun `opprettet er idempotent`() =
        withMigratedDb {
            opprettet()
            opprettet()

            graf.hent(sakId).size shouldBe 1
        }

    @Test
    fun `ferdig og avbrutt oppdaterer tilstanden`() =
        withMigratedDb {
            opprettet()
            tilstand(SøknadsbehandlingTilstand.Ferdig)
            graf.hent(sakId).single().tilstand shouldBe SøknadsbehandlingTilstand.Ferdig

            tilstand(SøknadsbehandlingTilstand.Avbrutt, ferdig.plusDays(1))
            graf.hent(sakId).single().tilstand shouldBe SøknadsbehandlingTilstand.Avbrutt
        }

    @Test
    fun `opprettet som kommer etter ferdig overskriver ikke tilstanden, men gir riktig innsendt`() =
        withMigratedDb {
            tilstand(SøknadsbehandlingTilstand.Ferdig)
            opprettet()

            graf.hent(sakId) shouldBe
                listOf(Søknadsbehandling(søknadId, sakId, opprettet, SøknadsbehandlingTilstand.Ferdig, behandlingId = behandlingId))
        }

    @Test
    fun `eldre tilstandshendelse overskriver ikke nyere`() =
        withMigratedDb {
            tilstand(SøknadsbehandlingTilstand.Avbrutt, ferdig)
            tilstand(SøknadsbehandlingTilstand.Ferdig, ferdig.minusDays(1))

            graf.hent(sakId).single().tilstand shouldBe SøknadsbehandlingTilstand.Avbrutt
        }

    @Test
    fun `ukjent ident og sak gir tom liste`() =
        withMigratedDb {
            graf.hent(ident) shouldBe emptyList()
            graf.hent(sakId) shouldBe emptyList()
        }
}
