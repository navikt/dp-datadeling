package no.nav.dagpenger.datadeling.ressurs

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.teknisk.LeaderElector
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RessursServiceTest {

    private val ressursDao: RessursDao = mockk()
    private val leaderElector: LeaderElector = mockk()

    private val ressursService = RessursService(
        ressursDao,
        leaderElector,
        RessursConfig(
            levetidMinutter = 10L,
            oppryddingsfrekvensMinutter = 10L,
        )
    )

    @BeforeAll
    fun setup() {
        every { leaderElector.isLeader() } returns true
    }

    @Test
    @Disabled
    // TODO: Skriv om denne testen så den fungerer
    fun `schedulerer opprydding av ressurser`() = runTest {
        every { ressursDao.slettFerdigeRessurser(any()) } returns 1

        launch {
            ressursService.scheduleRessursCleanup()
        }
        runBlocking {
            delay(20)
        }

        verify(exactly = 1) { ressursDao.slettFerdigeRessurser(any()) }
    }
}