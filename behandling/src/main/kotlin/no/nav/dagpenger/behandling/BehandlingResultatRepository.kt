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
}
