package no.nav.dagpenger.datadeling.api.config

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import no.nav.dagpenger.datadeling.MaskinportenConfig
import no.nav.dagpenger.datadeling.defaultLogger
import java.util.concurrent.TimeUnit

private data class Consumer(
    val authority: String,
    val ID: String,
)

fun ApplicationCall.orgNummer(): String {
    return principal<JWTPrincipal>()?.payload?.claims?.get("consumer")?.asMap()?.get("ID")?.let {
        it as String
    } ?: throw IllegalArgumentException("Fant ikke orgnummer i jwt")
}

fun AuthenticationConfig.maskinporten(
    name: String,
    maskinportenConfig: MaskinportenConfig,
) {
    val maskinportenJwkProvider: JwkProvider =
        JwkProviderBuilder(maskinportenConfig.jwksUri)
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    jwt(name) {
        verifier(maskinportenJwkProvider, maskinportenConfig.issuer)
        validate { cred ->
            if (cred.getClaim("scope", String::class) != maskinportenConfig.scope) {
                defaultLogger.warn("Wrong scope in claim")
                return@validate null
            }

            JWTPrincipal(cred.payload)
        }
    }
}
