package no.nav.dagpenger.datadeling.ressurs

import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

class RessursService(private val ressursDao: RessursDao) {
    fun opprett(request: DatadelingRequest) = ressursDao.opprett(request)

    fun hent(id: Long) = ressursDao.hent(id)

    fun ferdigstill(id: Long, response: DatadelingResponse) = ressursDao.ferdigstill(id, response)
}