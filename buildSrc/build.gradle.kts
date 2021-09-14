// https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin-dsl_plugin
plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "2.1.6"
}

repositories {
    mavenLocal()
    // The org.jetbrains.kotlin.jvm plugin requires a repository
    // where to download the Kotlin compiler dependencies from.
    maven {
        url = uri("https://nexus.figure.com/repository/mirror")
        credentials {
            username = findProperty("nexusUser")?.toString() ?: System.getenv("NEXUS_USER")
            password = findProperty("nexusPass")?.toString() ?: System.getenv("NEXUS_PASS")
        }
    }
}
