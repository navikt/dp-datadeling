project.setProperty("mainClassName", "no.nav.dagpenger.datadeling.AppKt")

val tokenValidationVersion = "4.1.0"
val kontrakterVersion = "2.0_20231222084529_f0d8240"
val mockOauth2Version = "2.1.1"
val wiremockVersion = "3.0.1"
val testcontainersVersion = "1.19.2"

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
    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-cio:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-metrics-micrometer:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")
    implementation(libs.micrometer.registry.prometheus)

    // Ktor Client
    implementation(libs.bundles.ktor.client)

    // Jackson
    implementation(libs.bundles.jackson)
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:${libs.versions.jackson.get()}")

    // Security
    implementation("no.nav.security:token-validation-ktor-v2:$tokenValidationVersion")

    // Log
    implementation(libs.kotlin.logging)

    // DB
    implementation(libs.bundles.postgres)

    // Nav
    implementation("no.nav.dagpenger.kontrakter:iverksett:$kontrakterVersion")
    implementation("no.nav.dagpenger.kontrakter:iverksett-integrasjoner:$kontrakterVersion")
    implementation(libs.dp.biblioteker.oauth2.klient)
    implementation(libs.rapids.and.rivers)
    implementation("no.nav.dagpenger:aktivitetslogg:20231003.21.027341")
    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(libs.testcontainer.postgresql)
    testImplementation("org.testcontainers:kafka:1.19.4")
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation("com.github.tomakehurst:wiremock-standalone:$wiremockVersion")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
}

application {
    mainClass.set(project.property("mainClassName").toString())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
