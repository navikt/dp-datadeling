/*
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.6/userguide/building_java_projects.html
 */

project.setProperty("mainClassName", "no.nav.dagpenger.datadeling.AppKt")

val micrometerVersion = "1.12.0"
val ktorVersion = "2.3.6"
val jacksonVersion = "2.16.0"
val openApiGeneratorVersion = "0.6.1"
val tokenValidationVersion = "3.1.8"
val kotlinLoggerVersion = "3.0.5"
val logbackVersion = "1.4.11"
val logstashVersion = "7.4"
val postgresVersion = "42.6.0"
val hikariVersion = "5.1.0"
val flywayVersion = "9.22.3"
val kontrakterVersion = "2.0_20230818151805_8ba7bfe"
val bibliotekerVersion = "2023.10.30-15.22.df63af45787f"
val mockOauth2Version = "2.0.0"
val jupiterVersion = "5.10.1"
val wiremockVersion = "3.0.1"
val testcontainersVersion = "1.19.2"
val mockkVersion = "1.13.8"
val hopliteVersion = "2.7.5"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.6"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        name = "github"
        url = uri("https://maven.pkg.github.com/navikt/dp-kontrakter")
        credentials {
            username = project.findProperty("githubUser") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("githubPassword") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")

    // Ktor Client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Jackson
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:$jacksonVersion")

    // OpenAPI / Swagger UI
    implementation("dev.forst:ktor-openapi-generator:$openApiGeneratorVersion")

    // Security
    implementation("no.nav.security:token-validation-ktor-v2:$tokenValidationVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    // Log
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggerVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // DB
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.github.seratch:kotliquery:1.9.0")

    // Nav
    implementation("no.nav.dagpenger.kontrakter:iverksett:$kontrakterVersion")
    implementation("no.nav.dagpenger.kontrakter:iverksett-integrasjoner:$kontrakterVersion")
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:$bibliotekerVersion")

    // Test
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.github.tomakehurst:wiremock-standalone:$wiremockVersion")
    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation("io.ktor:ktor-server-cio:$ktorVersion")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter:$jupiterVersion")
}

application {
    mainClass.set(project.property("mainClassName").toString())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
