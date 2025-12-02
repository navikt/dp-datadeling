package no.nav.dagpenger.datadeling.api.config

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import no.nav.dagpenger.datadeling.IssuerConfig
import no.nav.dagpenger.datadeling.defaultLogger
import java.util.concurrent.TimeUnit

fun ApplicationCall.orgNummer(): String =
    principal<JWTPrincipal>()
        ?.payload
        ?.claims
        ?.get("consumer")
        ?.asMap()
        ?.get("ID")
        ?.let { it as String }
        ?.parseISO6523ToOrgnummer()
        ?: throw IllegalArgumentException("Fant ikke orgnummer i jwt")

internal fun String.parseISO6523ToOrgnummer(): String {
    val strings = this.split(":")
    if (strings.size != 2) {
        throw IllegalArgumentException("Feil format på ISO6523-formatted string: $this")
    }
    val muligOrgnummer = strings.last()

    if (muligOrgnummer.length != 9) {
        throw IllegalArgumentException("Feil lengde på orgnummer. Forventet 9 siffer men var: $muligOrgnummer")
    }

    if (!muligOrgnummer.all { it.isDigit() }) {
        throw IllegalArgumentException("Orgnummer må bestå av kun siffer men var: $muligOrgnummer")
    }

    return muligOrgnummer
}

fun ApplicationCall.applikasjon(): String = principal<JWTPrincipal>()?.let { it.payload.claims["azp_name"]?.asString() } ?: "ukjent"

fun ApplicationCall.clientId(): String =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("azp")
        ?.asString()
        ?: throw IllegalArgumentException("Fant ikke clientId i jwt")

fun AuthenticationConfig.jwtAuth(
    name: String,
    config: IssuerConfig,
) {
    val jwkProvider =
        JwkProviderBuilder(config.jwksUri)
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    jwt(name) {
        verifier(jwkProvider, config.issuer)
        validate { cred ->
            // Scope i maskinporten token
            // Aud i Azure AD token
            if (cred.getClaim("scope", String::class) != config.scope &&
                cred.getClaim("aud", String::class) != config.scope
            ) {
                defaultLogger.warn { "Wrong scope/aud in claim" }
                return@validate null
            }

            JWTPrincipal(cred.payload)
        }
    }
}
