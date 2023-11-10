package no.nav.dagpenger.datadeling.teknisk

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import org.flywaydb.core.Flyway
import java.time.Duration
import javax.sql.DataSource

internal fun configureDataSource(config: ApplicationConfig): DataSource {
    val databaseHost: String = requireNotNull(config.property("DB_HOST").getString()) { "host må settes" }
    val databasePort: String = requireNotNull(config.property("DB_PORT").getString()) { "port må settes" }
    val databaseName: String = requireNotNull(config.property("DB_DATABASE").getString()) { "databasenavn må settes" }
    val databaseUsername: String = requireNotNull(config.property("DB_USERNAME").getString()) { "brukernavn må settes" }
    val databasePassword: String = requireNotNull(config.property("DB_PASSWORD").getString()) { "passord må settes" }

    val dbUrl = "jdbc:postgresql://$databaseHost:$databasePort/$databaseName"

    val hikariMigrationConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = databaseUsername
        password = databasePassword
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        maximumPoolSize = 2
    }

    val dataSource = HikariDataSource(hikariMigrationConfig)
    dataSource.use {
        Flyway.configure()
            .dataSource(it)
            .lockRetryCount(-1)
            .load()
            .migrate()
    }

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = databaseUsername
        password = databasePassword
        maximumPoolSize = 5
        minimumIdle = 2
        idleTimeout = Duration.ofMinutes(1).toMillis()
        maxLifetime = idleTimeout * 5
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        leakDetectionThreshold = Duration.ofSeconds(30).toMillis()
        metricRegistry = PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            CollectorRegistry.defaultRegistry,
            Clock.SYSTEM
        )
    }

    return HikariDataSource(hikariConfig)
}