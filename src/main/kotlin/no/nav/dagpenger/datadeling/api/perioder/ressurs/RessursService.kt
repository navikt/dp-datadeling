package no.nav.dagpenger.datadeling.api.perioder.ressurs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.RessursConfig
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

class RessursService(
    private val ressursDao: RessursDao,
    private val leaderElector: LeaderElector,
    private val config: RessursConfig,
) {
    fun opprett(request: DatadelingRequest) = ressursDao.opprett(request)

    fun hent(uuid: UUID) = ressursDao.hent(uuid)

    fun ferdigstill(
        uuid: UUID,
        response: DatadelingResponse,
    ) = ressursDao.ferdigstill(uuid, response)

    fun markerSomFeilet(uuid: UUID) = ressursDao.markerSomFeilet(uuid)

    suspend fun scheduleRessursCleanup(
        delay: Duration = Duration.ofMinutes(config.oppryddingsfrekvensMinutter),
        minutterLevetidOpprettet: Duration = Duration.ofHours(config.minutterLevetidOpprettet),
        minutterLevetidFerdig: Duration = Duration.ofHours(config.minutterLevetidFerdig),
    ) {
        schedule(delay) {
            if (!leaderElector.isLeader()) {
                return@schedule
            }
            logger.info("Starter opprydding av ressurser")
            val antallMarkertSomFeilet =
                ressursDao.markerSomFeilet(eldreEnn = LocalDateTime.now().minus(minutterLevetidOpprettet))
            logger.info("Markerte $antallMarkertSomFeilet ressurs(er) som feilet")
            val antallSlettet =
                ressursDao.slettFerdigeRessurser(eldreEnn = LocalDateTime.now().minus(minutterLevetidFerdig))
            logger.info("Slettet $antallSlettet ferdige og feilede ressurs(er)")
        }
    }

    private suspend fun schedule(
        delayDuration: Duration,
        action: () -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val scheduledEventFlow =
                flow {
                    while (true) {
                        emit(Unit)
                        delay(delayDuration.toMillis())
                    }
                }

            scheduledEventFlow.onEach { action() }.launchIn(this)
        }
    }
}
