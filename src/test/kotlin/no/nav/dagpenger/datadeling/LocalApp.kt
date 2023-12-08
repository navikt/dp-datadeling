package no.nav.dagpenger.datadeling

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import no.nav.dagpenger.datadeling.api.datadelingApi
import no.nav.dagpenger.datadeling.testutil.mockConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import javax.sql.DataSource

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.testModule(
    dataSource: DataSource = TestDatabase().dataSource,
    appConfig: AppConfig = mockConfig(8080, MockOAuth2Server()),
) {
    datadelingApi(
        config = appConfig,
        dataSource = dataSource
    )
}
