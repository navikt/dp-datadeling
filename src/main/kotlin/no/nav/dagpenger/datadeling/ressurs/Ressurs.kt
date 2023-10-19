package no.nav.dagpenger.datadeling.ressurs

import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

enum class RessursStatus {
    OPPRETTET,
    FERDIG,
    FEILET,
}

data class Ressurs(
    val id: Long,
    val status: RessursStatus,
    val data: DatadelingResponse?
)
