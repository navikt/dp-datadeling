package no.nav.dagpenger.datadeling

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.mockk.mockk
import no.nav.dagpenger.datadeling.testutil.mockConfig
import no.nav.dagpenger.datadeling.utils.loadConfig
import javax.sql.DataSource

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.testModule(
    dataSource: DataSource = TestDatabase().dataSource,
    port: Int = 8080,
    appConfig: AppConfig = mockConfig,
) {

    module(
        dataSource = dataSource,
        appConfig = appConfig,
        tokenProvider = mockk(relaxed = true)
    )
}
