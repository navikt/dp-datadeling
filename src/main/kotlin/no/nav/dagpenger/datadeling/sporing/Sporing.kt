package no.nav.dagpenger.datadeling.sporing

import no.nav.dagpenger.datadeling.objectMapper
import java.time.LocalDateTime
import java.util.Base64

internal class Sporing(
    val personIdent: String,
    val konsumentOrgNr: String,
    val dataForespørsel: Any,
    val leverteData: Any,
    private val behandlingsgrunnlag: String =
        "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
    private val tidspunkt: LocalDateTime = LocalDateTime.now(),
    private val tema: String = "DAG",
) {
    fun sporingHendelse(): String {
        val leverteDataBase64Encoded =
            Base64.getEncoder()
                .encodeToString(objectMapper.writeValueAsString(this.leverteData).encodeToByteArray())

        return """
            {
              "person": "${this.personIdent}",
              "mottaker": "${this.konsumentOrgNr}",
              "tema": "${this.tema}",
              "behandlingsGrunnlag": "${this.behandlingsgrunnlag}",
              "uthentingsTidspunkt": "${this.tidspunkt}",
              "dataForespoersel": ${this.dataForespørsel},
              "leverteData": "$leverteDataBase64Encoded"
            }
            """.trimIndent()
    }
}
