package no.nav.dagpenger.datadeling.api.config

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.datadeling.MaskinportenConfig
import no.nav.dagpenger.datadeling.defaultLogger
import java.util.concurrent.TimeUnit

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
