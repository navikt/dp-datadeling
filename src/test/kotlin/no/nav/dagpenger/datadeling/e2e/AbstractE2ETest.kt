package no.nav.dagpenger.datadeling.e2e

import no.nav.dagpenger.datadeling.TestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {
    private lateinit var testServerRuntime: TestServerRuntime
    private lateinit var testDatabase: TestDatabase

    protected val server get() = testServerRuntime
    protected val database get() = testDatabase
    protected val client get() = server.restClient()

    @BeforeAll
    fun setupServer() {
        testDatabase = TestDatabase()
        testServerRuntime = TestServer(database.dataSource).start()
    }

    @AfterAll
    fun tearDownServer() {
        testServerRuntime.close()
    }

    @BeforeEach
    fun resetDatabase() {
        database.reset()
    }
}