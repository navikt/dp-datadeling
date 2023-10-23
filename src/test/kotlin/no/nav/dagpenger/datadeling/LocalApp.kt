package no.nav.dagpenger.datadeling

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.mockk.mockk
import javax.sql.DataSource

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.testModule(
    dataSource: DataSource = mockk(relaxed = true),
    port: Int = 8080,
) {
    val config = MapApplicationConfig(
        "ENV" to "LOCAL",
        "DP_IVERKSETT_URL" to "http://0.0.0.0:8094",
        "DP_IVERKSETT_SCOPE" to "scope",
        "DP_PROXY_URL" to "http://0.0.0.0:8092",
        "DP_PROXY_SCOPE" to "scope",
        "DP_DATADELING_URL" to "http://localhost:$port",
        "AZURE_APP_WELL_KNOWN_URL" to "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration\"",
        "AZURE_APP_CLIENT_ID" to "test",
        "no.nav.security.jwt.issuers.size" to "1",
        "no.nav.security.jwt.issuers.0.issuer_name" to "default",
        "no.nav.security.jwt.issuers.0.discoveryurl" to "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration\"",
        "no.nav.security.jwt.issuers.0.accepted_audience" to "default"
    )

    module(
        dataSource = dataSource,
        appConfig = AppConfig.fra(config),
        tokenProvider = mockk(relaxed = true)
    )
}
