package no.nav.dagpenger.datadeling.api.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import no.nav.dagpenger.datadeling.api.config.applicationId
import no.nav.dagpenger.datadeling.api.config.applikasjonsroller
import no.nav.dagpenger.datadeling.api.config.konsument

private val sikkerlogger = KotlinLogging.logger("tjenestekall")
private val logger = KotlinLogging.logger {}

val AuthorizationPlugin =
    createRouteScopedPlugin(
        name = "AuthorizationPlugin",
        createConfiguration = ::PluginConfiguration,
    ) {
        val tilganger = pluginConfig.tilganger
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                val konsument = call.konsument()

                // azure-token-generator har ikke roller, men skal ha tilgang til alt for testing
                // todo: Bør styres av konfigurasjon i fremtiden?
                if (konsument == "dev-gcp:nais:azure-token-generator") {
                    return@on
                }

                val roller: Set<String> = call.applikasjonsroller
                if (roller.none { it in tilganger }) {
                    sikkerlogger.warn {
                        "$konsument (${call.applicationId}) mangler tilgang til ${tilganger.joinToString(
                            ", " ,
                            "'",
                            "'",
                        )}"
                    }
                    logger.warn {
                        "Avslår kall fra $konsument uten nødvendig tilgang (se sikkerlogg for detaljer)"
                    }
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }
    }

class PluginConfiguration {
    var tilganger: Set<String> = emptySet()
}
