package no.nav.dagpenger.datadeling.teknisk

import kotliquery.queryOf
import org.intellij.lang.annotations.Language

fun asQuery(@Language("SQL") sql: String, argMap: Map<String, Any?> = emptyMap()) = queryOf(sql, argMap)

fun asQuery(@Language("SQL") sql: String, vararg params: Any?) = queryOf(sql, *params)