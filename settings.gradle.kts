rootProject.name = "service-asset-onboarding"

pluginManagement {
    // builds before buildSrc extensions are available :(
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
}

include(
    "sdk",
    //"cli",
    "service"
)
