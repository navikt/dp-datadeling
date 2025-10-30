plugins {
    `kotlin-dsl`
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.0")
}
