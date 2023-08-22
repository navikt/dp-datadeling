package dp.datadeling.api

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import dp.datadeling.defaultLogger
import dp.datadeling.logic.process
import dp.datadeling.utils.auth
import dp.datadeling.utils.respondError
import dp.datadeling.utils.respondOk
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDate
import com.papsign.ktor.openapigen.route.path.auth.post as authPost


fun NormalOpenAPIRoute.dataApi() {

    auth {
        route("/data/v1.0") {
            authPost<Unit, DatadelingResponse, DatadelingRequest, TokenValidationContextPrincipal?>(
                info("Oppslag"),
                exampleRequest = dataRequestExample,
                exampleResponse = dataResponseExample
            ) { _, request ->
                try {
                    val response = process(request)

                    respondOk(response)
                } catch (e: Exception) {
                    // Feil? Logg og svar med status 500
                    defaultLogger.error { e }
                    respondError("Kunne ikke få data", e)
                }
            }
        }
    }
}

private val dataRequestExample = DatadelingRequest(
    personIdent = "01020312345",
    fraOgMedDato = LocalDate.now(),
    tilOgMedDato = LocalDate.now()
)

private val dataResponseExample = DatadelingResponse(
    personIdent = "01020312345",
    perioder = listOf(
        Periode(
            fraOgMedDato = LocalDate.now(),
            tilOgMedDato = LocalDate.now(),
            ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
            gjenståendeDager = 0
        )
    )
)
