plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.0.4"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.4")
}
