import org.gradle.internal.impldep.com.amazonaws.util.json.Jackson

plugins {
    id("ch.acanda.gradle.fabrikt") version "1.27.1"
    id("common")
    `java-library`
}

tasks {
    compileKotlin {
        dependsOn("fabriktGenerateDatadeling")
    }
}

tasks.named("runKtlintCheckOverMainSourceSet").configure {
    dependsOn("fabriktGenerateDatadeling")
}

tasks.named("runKtlintFormatOverMainSourceSet").configure {
    dependsOn("fabriktGenerateDatadeling")
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
    implementation(libs.jackson.annotation)
}

fabrikt {
    generate("datadeling") {
        apiFile = file("$projectDir/src/main/resources/datadeling-api.yaml")
        basePackage = "no.nav.dagpenger.datadeling"
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
            suffix = "DTO"
        }
    }
}
