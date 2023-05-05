/*
 * (C) 2021 GoodData Corporation
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.XML

plugins {
    kotlin("jvm")
    idea
    `java-library`
    `maven-publish`

    id("pl.allegro.tech.build.axion-release")
    id("io.gitlab.arturbosch.detekt")
    id("org.sonarqube")
    id("org.owasp.dependencycheck")

    id("org.jetbrains.dokka") apply false
    // retrieve maven credentials to publish in nexus, only applied for subprojects
    id("org.datlowe.maven-publish-auth") apply false
}

allprojects {
    apply(plugin = "idea")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.owasp.dependencycheck")

    group = "com.gooddata.api"

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }

    java.sourceCompatibility = JavaVersion.VERSION_11

    dependencyCheck { formats = listOf(XML, HTML) }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "pl.allegro.tech.build.axion-release")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    scmVersion {
        tag {
            prefix = "${project.name}-"
        }
    }

    project.version = scmVersion.version

    detekt {
        source = files(
            "src/main/kotlin",
            "src/test/kotlin"
        )
        config = files(
            "$rootDir/gradle/scripts/detekt-config.yml",
            "$rootDir/gradle/scripts/detekt-config-strict.yml"
        )
    }

    dependencies {
        val detektVersion: String by project

        implementation(kotlin("stdlib-jdk8"))

        // Tests
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

        // Mocking library
        testImplementation("io.mockk:mockk:1.12.4")

        // Assertions library
        testImplementation("io.strikt:strikt-core:0.34.1")

        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    }

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }


    tasks {
        withType<Test> {
            useJUnitPlatform()
        }

        withType<KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=enable", "-Xopt-in=kotlin.RequiresOptIn")
                jvmTarget = "11"
            }
        }

        register<Jar>("dokkaJavadocJar") {
            dependsOn("dokkaJavadoc")
            archiveClassifier.set("javadoc")
            from(javadoc)
        }
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("library") {
                from(components["java"])
            }
        }
    }
}
