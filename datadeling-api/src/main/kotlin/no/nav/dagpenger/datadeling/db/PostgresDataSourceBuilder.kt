package no.nav.dagpenger.datadeling.db

import ch.qos.logback.core.util.OptionHelper.getEnv
import ch.qos.logback.core.util.OptionHelper.getSystemProperty
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.internal.configuration.ConfigUtils
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal object PostgresDataSourceBuilder {
    const val DB_USERNAME_KEY = "DB_USERNAME"
    const val DB_PASSWORD_KEY = "DB_PASSWORD"
    const val DB_DATABASE_KEY = "DB_DATABASE"
    const val DB_HOST_KEY = "DB_HOST"
    const val DB_PORT_KEY = "DB_PORT"

    private fun getOrThrow(key: String): String = getEnv(key) ?: getSystemProperty(key)

    val dataSource by lazy {
        HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", getOrThrow(DB_HOST_KEY))
            addDataSourceProperty("portNumber", getOrThrow(DB_PORT_KEY))
            addDataSourceProperty("databaseName", getOrThrow(DB_DATABASE_KEY))
            addDataSourceProperty("user", getOrThrow(DB_USERNAME_KEY))
            addDataSourceProperty("password", getOrThrow(DB_PASSWORD_KEY))
            // Default 30 sekund
            connectionTimeout = 10.seconds.inWholeMilliseconds
            // Default 5 minutter
            idleTimeout = 5.minutes.inWholeMilliseconds
            // Default 2 minutter
            keepaliveTime = 2.minutes.inWholeMilliseconds
            // Default 30 minutter
            maxLifetime = 30.minutes.inWholeMilliseconds
            metricRegistry =
                PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    PrometheusRegistry.defaultRegistry,
                    Clock.SYSTEM,
                )
        }
    }

    private fun flyWayBuilder() = Flyway.configure().connectRetries(10)

    private val flyWayBuilder: FluentConfiguration = Flyway.configure().connectRetries(10)

    fun clean() =
        flyWayBuilder
            .cleanDisabled(
                getOrThrow(ConfigUtils.CLEAN_DISABLED).toBooleanStrict(),
            ).dataSource(dataSource)
            .load()
            .clean()

    internal fun runMigration(initSql: String? = null): Int =
        flyWayBuilder
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
            .migrations
            .size

    internal fun runMigrationTo(target: String): Int =
        flyWayBuilder()
            .dataSource(dataSource)
            .target(target)
            .load()
            .migrate()
            .migrations
            .size
}
