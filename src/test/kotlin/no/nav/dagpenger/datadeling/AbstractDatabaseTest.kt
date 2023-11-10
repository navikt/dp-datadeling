package no.nav.dagpenger.datadeling

import org.junit.jupiter.api.BeforeEach

abstract class AbstractDatabaseTest {
    protected val database = TestDatabase()

    @BeforeEach
    fun resetDatabase() {
        database.reset()
    }
}