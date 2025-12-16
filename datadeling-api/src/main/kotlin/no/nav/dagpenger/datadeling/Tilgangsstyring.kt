package no.nav.dagpenger.datadeling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.path

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

fun ApplicationCall.håndhevTilgangTil(påkrevdRolle: String) = håndhevTilgangTil(setOf(påkrevdRolle))

fun ApplicationCall.håndhevTilgangTil(enAvRollene: Set<String>) {
    val endepunkt = this.request.path()
    val rolle =
        enAvRollene.firstOrNull { it in roles } ?: run {
            val feilmelding =
                "Applikasjonen $applicationName ($applicationId) har ikke tilgang til $endepunkt - Må ha en av rollene $enAvRollene, har bare $roles"
            sikkerlogger.error { feilmelding }
            throw UnauthorizedException(feilmelding)
        }

    sikkerlogger.info { "Håndterer request til $endepunkt fra $applicationName ($applicationId) som har rolle $rolle" }
}

private val ApplicationCall.applicationName
    get() =
        this
            .principal<JWTPrincipal>()
            ?.getClaim("azp_name", String::class)
            .takeUnless { it.isNullOrBlank() }
            ?: "n/a"

private val ApplicationCall.applicationId
    get() =
        this
            .principal<JWTPrincipal>()
            ?.getClaim("azp", String::class)
            .takeUnless { it.isNullOrBlank() }
            ?: "n/a"

private val ApplicationCall.roles
    get() =
        this
            .principal<JWTPrincipal>()
            ?.getListClaim("roles", String::class)
            ?: emptyList()

class UnauthorizedException(
    message: String,
) : RuntimeException(message)
