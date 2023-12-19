package no.nav.dagpenger.datadeling.api.perioder.ressurs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import java.util.UUID

enum class RessursStatus {
    OPPRETTET,
    FERDIG,
    FEILET,
}

@JsonIgnoreProperties(value = ["request"], allowGetters = false, allowSetters = true)
data class Ressurs(
    val uuid: UUID,
    val status: RessursStatus,
    val request: DatadelingRequest,
    val response: DatadelingResponse?,
)
