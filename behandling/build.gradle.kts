plugins {
    id("ch.acanda.gradle.fabrikt") version "1.27.1"
    id("common")
    `java-test-fixtures`
}

tasks {
    compileKotlin {
        dependsOn("fabriktGenerateBehandlingV1")
    }
}

// Forhindrer parallell kjøring med openapi:fabriktGenerateDatadeling fordi
// fabrikt bruker globale mutable settings (MutableSettings singleton) som
// fører til at konfigurasjoner "smitter" mellom prosjekter ved parallell kjøring.
afterEvaluate {
    tasks.named("fabriktGenerateBehandlingV1") {
        mustRunAfter(":openapi:fabriktGenerateDatadeling")
    }
}

tasks.named("runKtlintCheckOverMainSourceSet").configure {
    dependsOn("fabriktGenerateBehandlingV1")
}

tasks.named("runKtlintFormatOverMainSourceSet").configure {
    dependsOn("fabriktGenerateBehandlingV1")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin", "${layout.buildDirectory.get()}/generated/src/main/kotlin"))
        }
    }
}

ktlint {
    filter {
        exclude { element -> element.file.path.contains("generated") }
    }
}

dependencies {
    implementation(project(path = ":openapi"))
    implementation(project(path = ":dato"))
    implementation(project(path = ":ktor-client"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.jackson)

    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.rapids.and.rivers.test)
}

fabrikt {
    generate("behandling-v1") {
        apiFile = file("$projectDir/src/main/resources/behandling-api-v1.yaml")
        basePackage = "no.nav.dagpenger.behandling.kontrakt.v1"
        skip = false
        quarkusReflectionConfig = disabled
        typeOverrides {
            datetime = LocalDateTime
        }
        model {
            generate = enabled
            validationLibrary = NoValidation
            extensibleEnums = disabled
            sealedInterfacesForOneOf = enabled
            ignoreUnknownProperties = disabled
            nonNullMapValues = enabled
            serializationLibrary = Jackson
            suffix = "v1DTO"
        }
    }
}
