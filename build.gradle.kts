project.setProperty("mainClassName", "no.nav.dagpenger.datadeling.AppKt")

val kontrakterVersion = "3.0_20240408122747_6eff346"
val mockOauth2Version = "2.1.8"
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

    // Ktor Client
    implementation(libs.bundles.ktor.client)

    // Jackson
    implementation(libs.bundles.jackson)
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:${libs.versions.jackson.get()}")

    // Log
    implementation(libs.kotlin.logging)

    // DB
    implementation(libs.bundles.postgres)

    // Nav
    implementation("no.nav.dagpenger.kontrakter:iverksett:$kontrakterVersion")
    implementation("no.nav.dagpenger.kontrakter:iverksett-integrasjoner:$kontrakterVersion")
    implementation(libs.dp.biblioteker.oauth2.klient)
    implementation(libs.rapids.and.rivers)
    implementation("no.nav.dagpenger:aktivitetslogg:20240412.29.afd090")
    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(libs.testcontainer.postgresql)
    testImplementation("org.testcontainers:kafka:1.20.1")
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation("com.github.tomakehurst:wiremock-standalone:$wiremockVersion")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
}

application {
    mainClass.set(project.property("mainClassName").toString())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
