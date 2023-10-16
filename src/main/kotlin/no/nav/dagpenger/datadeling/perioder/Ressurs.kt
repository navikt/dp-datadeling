package no.nav.dagpenger.datadeling.perioder

enum class RessursStatus {
    OPPRETTET,
    FERDIG,
    FEILET,
}

data class Ressurs<T>(
    val id: Long,
    val status: RessursStatus,
    val data: T?
)
