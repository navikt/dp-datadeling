package no.nav.dagpenger.datadeling.ressurs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.teknisk.LeaderElector
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

class RessursService(
    private val ressursDao: RessursDao,
    private val leaderElector: LeaderElector,
    private val config: RessursConfig,
) {
    fun opprett(request: DatadelingRequest) = ressursDao.opprett(request)

    fun hent(uuid: UUID) = ressursDao.hent(uuid)

    fun ferdigstill(uuid: UUID, response: DatadelingResponse) = ressursDao.ferdigstill(uuid, response)

    suspend fun scheduleRessursCleanup(
        delay: Duration = Duration.ofMinutes(config.oppryddingsfrekvensMinutter),
        timeToLive: Duration = Duration.ofHours(config.levetidMinutter),
    ) {
        schedule(delay) {
            if (!leaderElector.isLeader()) {
                return@schedule
            }
            logger.info("Starter sletting av ferdige ressurser")
            val antallSlettet = ressursDao.slettFerdigeRessurser(
                eldreEnn = LocalDateTime.now().minus(timeToLive)
            )
            logger.info("Slettet $antallSlettet ressurs(er)")
        }
    }

    private suspend fun schedule(delayDuration: Duration, action: () -> Unit) {
        withContext(Dispatchers.IO) {
            val scheduledEventFlow = flow {
                while (true) {
                    emit(Unit)
                    delay(delayDuration.toMillis())
                }
            }

            scheduledEventFlow.onEach { action() }.launchIn(this)
        }
    }
}