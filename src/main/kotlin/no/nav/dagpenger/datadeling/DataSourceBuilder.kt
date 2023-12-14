package no.nav.dagpenger.datadeling

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import org.flywaydb.core.Flyway
import java.time.Duration
import javax.sql.DataSource

internal fun configureDataSource(config: DbConfig): DataSource {
    val dbUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.name}"

    val hikariMigrationConfig =
        HikariConfig().apply {
            jdbcUrl = dbUrl
            username = config.username
            password = config.password
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

    val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = dbUrl
            username = config.username
            password = config.password
            maximumPoolSize = 5
            minimumIdle = 2
            idleTimeout = Duration.ofMinutes(1).toMillis()
            maxLifetime = idleTimeout * 5
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofSeconds(5).toMillis()
            leakDetectionThreshold = Duration.ofSeconds(30).toMillis()
            metricRegistry =
                PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    CollectorRegistry.defaultRegistry,
                    Clock.SYSTEM,
                )
        }

    return HikariDataSource(hikariConfig)
}
