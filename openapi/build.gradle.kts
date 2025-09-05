plugins {
    id("org.openapi.generator") version "7.15.0"
    id("common")
    `java-library`
}

tasks.named("compileKotlin").configure {
    dependsOn("openApiGenerate")
}

tasks.named("spotlessKotlin").configure {
    dependsOn("openApiGenerate")
}

sourceSets {
    main {
        java {
            setSrcDirs(
                listOf(
                    "src/main/kotlin",
                    layout.buildDirectory
                        .dir("generated/src/main/kotlin")
                        .get()
                        .asFile
                        .path,
                ),
            )
        }
    }
}

spotless {
    kotlin {
        targetExclude("**/generated/**")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

openApiGenerate {
    generatorName.set("kotlin-server")
    inputSpec.set("$projectDir/src/main/resources/datadeling-api.yaml")
    outputDir.set(
        layout.buildDirectory
            .dir("generated/")
            .get()
            .asFile
            .path,
    )
    packageName.set("no.nav.dagpenger.datadeling")
    globalProperties.set(
        mapOf(
            "apis" to "none",
            "models" to "",
        ),
    )
    modelNameSuffix.set("DTO")
    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "original",
        ),
    )
}
