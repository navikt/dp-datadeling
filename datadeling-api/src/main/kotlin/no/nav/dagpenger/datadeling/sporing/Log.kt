package no.nav.dagpenger.datadeling.sporing

interface Log {
    fun log(hendelse: AuditHendelse)
}

abstract class AuditLog : Log {
    override fun log(hendelse: AuditHendelse) {
        audit(hendelse)
    }

    abstract fun audit(hendelse: AuditHendelse)
}
