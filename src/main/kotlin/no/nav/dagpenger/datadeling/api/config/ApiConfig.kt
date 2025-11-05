package no.nav.dagpenger.datadeling.api.config

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.objectMapper

fun Application.konfigurerApi(appConfig: AppConfig) {
    install(Authentication) {
        jwtAuth(name = "afpPrivat", config = appConfig.maskinporten)
        jwtAuth(name = "azure", config = appConfig.azure)
    }

    install(ContentNegotiation) {
        jackson {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
    }
}
