plugins {
    `kotlin-dsl`
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("org.jlleitschuh.gradle:ktlint-gradle:13.1.0")
}
