package no.nav.dagpenger.behandling

import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

interface BehandlingResultatRepository {
    fun lagre(
        ident: String,
        behandlingId: UUID,
        basertPåId: UUID?,
        sakId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    )

    fun oppdater(
        nyId: UUID,
        gammelId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    )

    fun ny(
        ident: String,
        behandlingId: UUID,
        sakId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    )

    fun hent(ident: String): List<JsonNode>

    fun hentMedSak(ident: String): List<LagretBehandlingResultat>

    fun hentMedSakId(sakId: UUID): List<LagretBehandlingResultat>

    fun hentMedBehandlingId(behandlingId: UUID): LagretBehandlingResultat?
}

data class LagretBehandlingResultat(
    val sakId: UUID,
    val opprettet: LocalDateTime,
    val json: JsonNode,
)
