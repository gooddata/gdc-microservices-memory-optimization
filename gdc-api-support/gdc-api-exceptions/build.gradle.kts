/*
 * (C) 2021 GoodData Corporation
 */

plugins {
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    val kotlinVersion: String by System.getProperties()

    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}
