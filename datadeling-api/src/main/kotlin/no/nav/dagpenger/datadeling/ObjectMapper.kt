package no.nav.dagpenger.datadeling

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

val objectMapper =
    JsonMapper
        .builder()
        .addModule(
            KotlinModule
                .Builder()
                .build(),
        ).configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .changeDefaultPropertyInclusion { inclusion ->
            inclusion.withContentInclusion(JsonInclude.Include.NON_NULL)
            inclusion.withValueInclusion(JsonInclude.Include.NON_NULL)
        }.build()
