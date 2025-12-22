package no.nav.dagpenger.datadeling.api.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import no.nav.dagpenger.datadeling.api.config.applicationId
import no.nav.dagpenger.datadeling.api.config.applikasjonsroller
import no.nav.dagpenger.datadeling.api.config.konsument

private val sikkerlogger = KotlinLogging.logger("AuthorizationPlugin.tjenestekall")

val AuthorizationPlugin =
    createRouteScopedPlugin(
        name = "AuthorizationPlugin",
        createConfiguration = ::PluginConfiguration,
    ) {
        val tilganger = pluginConfig.tilganger
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                val roller: Set<String> = call.applikasjonsroller
                if (roller.none { it in tilganger }) {
                    sikkerlogger.warn {
                        "${call.konsument()} (${call.applicationId}) mangler tilgang til ${tilganger.joinToString(
                            ", " ,
                            "'",
                            "'",
                        )}"
                    }
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }
    }

class PluginConfiguration {
    var tilganger: Set<String> = emptySet()
}
