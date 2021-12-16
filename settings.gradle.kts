rootProject.name = "service-asset-onboarding"

pluginManagement {
    // builds before buildSrc extensions are available :(
    repositories {
        val nexusUser: String? by settings
        val nexusPass: String? by settings
        maven {
            url = uri("https://nexus.figure.com/repository/mirror")
            credentials {
                username = nexusUser ?: System.getenv("NEXUS_USER")
                password = nexusPass ?: System.getenv("NEXUS_PASS")
            }
        }
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
}

include(
    "sdk",
    "cli",
    "service"
)
