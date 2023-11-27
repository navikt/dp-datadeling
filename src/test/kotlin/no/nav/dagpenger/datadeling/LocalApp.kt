package no.nav.dagpenger.datadeling

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.mockk.mockk
import no.nav.dagpenger.datadeling.testutil.mockConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import javax.sql.DataSource

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.testModule(
    dataSource: DataSource = TestDatabase().dataSource,
    appConfig: AppConfig = mockConfig(8080, MockOAuth2Server()),
) {
    module(
        dataSource = dataSource,
        appConfig = appConfig,
        tokenProvider = mockk(relaxed = true)
    )
}
