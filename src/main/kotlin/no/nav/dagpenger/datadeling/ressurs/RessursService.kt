package no.nav.dagpenger.datadeling.ressurs

import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import java.util.UUID

class RessursService(private val ressursDao: RessursDao) {
    fun opprett(request: DatadelingRequest) = ressursDao.opprett(request)

    fun hent(uuid: UUID) = ressursDao.hent(uuid)

    fun ferdigstill(uuid: UUID, response: DatadelingResponse) = ressursDao.ferdigstill(uuid, response)
}