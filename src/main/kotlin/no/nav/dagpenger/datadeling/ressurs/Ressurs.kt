package no.nav.dagpenger.datadeling.ressurs

import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import java.util.*

enum class RessursStatus {
    OPPRETTET,
    FERDIG,
    FEILET,
}

data class Ressurs(
    val uuid: UUID,
    val status: RessursStatus,
    val response: DatadelingResponse?
)
