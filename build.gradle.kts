/*
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.6/userguide/building_java_projects.html
 */

project.setProperty("mainClassName", "dp.datadeling.AppKt")

val ktorVersion = "2.3.0"
val micrometerVersion = "1.10.6"
val jacksonVersion = "2.15.0"
val openApiGeneratorVersion = "0.6.1"
val tokenValidationVersion = "3.0.11"
val kotlinLoggerVersion = "3.0.5"
val logbackVersion = "1.4.7"
val logstashVersion = "7.3"
val postgresVersion = "42.6.0"
val hikariVersion = "5.0.1"
val flywayVersion = "9.18.0"
val mockOauth2Version = "0.5.8"
val jupiterVersion = "5.9.3"
val testcontainersVersion = "1.18.0"
val mockkVersion = "1.13.5"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    // Apply io.ktor.plugin to build a fat JAR
    id("io.ktor.plugin") version "2.3.0"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")

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

    // Test
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    // Testcontainers
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    // MockK
    testImplementation("io.mockk:mockk:$mockkVersion")
}

application {
    // Define the main class for the application.
    mainClass.set(project.property("mainClassName").toString())
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks {
    register("runServerTest", JavaExec::class) {
        environment["AZURE_APP_WELL_KNOWN_URL"] = "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration"
        environment["AZURE_APP_CLIENT_ID"] = "test"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(project.property("mainClassName").toString())
    }
}
