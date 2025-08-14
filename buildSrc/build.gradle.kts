plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.2.1"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.2.1")
}
