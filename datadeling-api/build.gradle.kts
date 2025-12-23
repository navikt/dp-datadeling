val mockOauth2Version = "3.0.1"

plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation(project(path = ":openapi"))
    implementation(project(path = ":soknad"))
    implementation(project(path = ":meldekort"))
    implementation(project(path = ":behandling"))
    implementation(project(path = ":ktor-client"))
    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-cio:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-metrics-micrometer:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")

    // Naisful app
    implementation("com.github.navikt.tbd-libs:naisful-app:2025.11.04-10.54-c831038e")

    // Prometheus open metrics
    implementation("io.prometheus:prometheus-metrics-core:1.4.2")

    // OpenTelemetry
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
    implementation("io.opentelemetry:opentelemetry-api:1.36.0")

    // Jackson
    implementation(libs.bundles.jackson)
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:${libs.versions.jackson.get()}")

    // Log
    implementation(libs.kotlin.logging)

    // DB
    implementation(libs.bundles.postgres)

    // Nav
    implementation("no.nav.dagpenger:oauth2-klient:2025.12.19-08.15.2e150cd55270")
    implementation(libs.rapids.and.rivers)
    implementation("no.nav.dagpenger:aktivitetslogg:20251016.40.a3c526")
    implementation("io.ktor:ktor-server-metrics:3.3.1")

    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:2025.11.04-10.54-c831038e")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation(libs.testcontainer.postgresql)
    testImplementation("org.testcontainers:kafka:1.21.3")
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
    testImplementation(testFixtures(project(":behandling")))
}

application {
    mainClass.set("no.nav.dagpenger.datadeling.AppKt")
}
