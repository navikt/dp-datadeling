/*
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.6/userguide/building_java_projects.html
 */

project.setProperty("mainClassName", "dp.datadeling.AppKt")

val ktorVersion = "2.3.3"
val micrometerVersion = "1.11.3"
val jacksonVersion = "2.15.2"
val openApiGeneratorVersion = "0.6.1"
val tokenValidationVersion = "3.1.5"
val kotlinLoggerVersion = "3.0.5"
val logbackVersion = "1.4.11"
val logstashVersion = "7.4"
val postgresVersion = "42.6.0"
val hikariVersion = "5.0.1"
val flywayVersion = "9.22.0"
val kontrakterVersion = "2.0_20230818151805_8ba7bfe"
val bibliotekerVersion = "2023.04.27-09.33.fcf0798bf943"
val mockOauth2Version = "1.0.0"
val jupiterVersion = "5.10.0"
val testcontainersVersion = "1.19.0"
val mockkVersion = "1.13.7"
val wiremockVersion = "3.0.0"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.3"

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
    implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

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

    // Log
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggerVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // DB
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    // Kontrakter
    implementation("no.nav.dagpenger.kontrakter:iverksett:$kontrakterVersion")
    implementation("no.nav.dagpenger.kontrakter:iverksett-integrasjoner:$kontrakterVersion")

    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:$bibliotekerVersion")

    // Test
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.github.tomakehurst:wiremock-standalone:$wiremockVersion")
    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
}

application {
    mainClass.set(project.property("mainClassName").toString())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks {
    register("runServerTest", JavaExec::class) {
        environment["ENV"] = "LOCAL"
        environment["DP_IVERKSETT_URL"] = "http://localhost:8094/api"

        environment["AZURE_APP_WELL_KNOWN_URL"] =
            "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration"
        environment["AZURE_APP_CLIENT_ID"] = "test"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(project.property("mainClassName").toString())
    }
}
