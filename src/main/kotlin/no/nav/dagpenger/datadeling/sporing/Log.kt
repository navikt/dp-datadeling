package no.nav.dagpenger.datadeling.sporing

interface Log {
    fun log(hendelse: AuditHendelse)
}

abstract class SporingOgAudit : Log {
    override fun log(hendelse: AuditHendelse) {
        audit(hendelse)
        spor(hendelse)
    }

    abstract fun audit(hendelse: AuditHendelse)

    abstract fun spor(hendelse: AuditHendelse)
}
