import kotlin.math.max

plugins {
    kotlin("jvm") version Versions.Kotlin apply false
    id("java")
}

allprojects {
    val project = this
    group = "com.figure.lending"
    version = figureArtifactVersion()

    repositories {
        figureNexusMirrorRepository(project)
    }
}

subprojects {
    apply {
        Plugins.Kotlin.addTo(this)
        Plugins.Idea.addTo(this)
        plugin("java")
    }

    repositories {
        figureNexusMirrorRepository(project)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"

        kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
            jvmTarget = "11"
            allWarningsAsErrors = false
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
