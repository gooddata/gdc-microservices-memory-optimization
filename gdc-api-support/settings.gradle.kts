/*
 * (C) 2021 GoodData Corporation
 */

rootProject.name = "gdc-api-support"
include("gdc-api-exceptions")
include("gdc-api-logging")


pluginManagement {
    plugins {
        val detektVersion: String by settings
        val kotlinVersion: String by System.getProperties()

        id("org.owasp.dependencycheck") version "7.1.2"

        kotlin("jvm") version kotlinVersion

        id("pl.allegro.tech.build.axion-release") version "1.13.14"
        id("org.jetbrains.dokka") version "1.7.10"

        id("org.datlowe.maven-publish-auth") version "2.0.2"

        id("io.gitlab.arturbosch.detekt") version detektVersion
        id("org.sonarqube") version "3.4.0.2513"
    }
}
