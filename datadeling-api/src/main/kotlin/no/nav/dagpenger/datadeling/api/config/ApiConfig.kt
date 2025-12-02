package no.nav.dagpenger.datadeling.api.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import no.nav.dagpenger.datadeling.AppConfig

fun Application.authentication(appConfig: AppConfig) {
    install(Authentication) {
        jwtAuth(name = "afpPrivat", config = appConfig.maskinporten)
        jwtAuth(name = "azure", config = appConfig.azure)
    }
}
