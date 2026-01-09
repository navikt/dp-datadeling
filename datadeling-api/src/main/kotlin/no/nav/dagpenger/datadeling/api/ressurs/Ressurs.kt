package no.nav.dagpenger.datadeling.api.ressurs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseAfpDTO
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
    val request: DatadelingRequestDTO,
    val response: DatadelingResponseAfpDTO?,
)
