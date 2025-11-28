

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

    // Jackson
    implementation(libs.bundles.jackson)
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:${libs.versions.jackson.get()}")

    // Log
    implementation(libs.kotlin.logging)

    // DB
    implementation(libs.bundles.postgres)

    // Nav
    implementation("no.nav.dagpenger:oauth2-klient:2025.11.27-14.20.4aa9aa3d3d14")
    implementation(libs.rapids.and.rivers)
    implementation("no.nav.dagpenger:aktivitetslogg:20251016.40.a3c526")

    // Test
    testImplementation(libs.ktor.server.test.host)
    implementation("com.github.navikt.tbd-libs:naisful-test-app:2025.11.04-10.54-c831038e")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation(libs.testcontainer.postgresql)
    testImplementation("org.testcontainers:kafka:1.21.3")
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
    testImplementation(project(":behandling"))
}

sourceSets {
    test {
        resources.srcDir(project(":behandling").projectDir.resolve("src/test/resources"))
    }
}

application {
    mainClass.set("no.nav.dagpenger.datadeling.AppKt")
}
