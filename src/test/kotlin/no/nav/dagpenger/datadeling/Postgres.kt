package no.nav.dagpenger.datadeling

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.testcontainers.containers.PostgreSQLContainer

internal object Postgres {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:14").apply {
            start()
        }
    }

    fun withMigratedDb(block: () -> Unit) {
        withCleanDb {
            PostgresDataSourceBuilder.runMigration()
            block()
        }
    }

    fun withMigratedDb(): HikariDataSource {
        setup()
        PostgresDataSourceBuilder.runMigration()
        return PostgresDataSourceBuilder.dataSource
    }

    fun setup() {
        System.setProperty(ConfigUtils.CLEAN_DISABLED, "false")
        System.setProperty(PostgresDataSourceBuilder.DB_HOST_KEY, instance.host)
        System.setProperty(
            PostgresDataSourceBuilder.DB_PORT_KEY,
            instance.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT).toString(),
        )
        System.setProperty(PostgresDataSourceBuilder.DB_DATABASE_KEY, instance.databaseName)
        System.setProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY, instance.password)
        System.setProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY, instance.username)
    }

    fun tearDown() {
        System.clearProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_HOST_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_PORT_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_DATABASE_KEY)
        System.clearProperty(ConfigUtils.CLEAN_DISABLED)
    }

    fun withCleanDb(block: () -> Unit) {
        setup()
        PostgresDataSourceBuilder.clean().run {
            block()
        }.also {
            tearDown()
        }
    }

    fun withCleanDb(
        target: String,
        setup: () -> Unit,
        test: () -> Unit,
    ) {
        Postgres.setup()
        PostgresDataSourceBuilder.clean().run {
            PostgresDataSourceBuilder.runMigrationTo(target)
            setup()
            PostgresDataSourceBuilder.runMigration()
            test()
        }.also {
            tearDown()
        }
    }
}
