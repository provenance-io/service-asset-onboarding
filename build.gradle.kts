plugins {
    kotlin("jvm") version Versions.Kotlin apply false
    id("java")
    `maven-publish`
}

allprojects {
    val project = this
    group = "tech.figure.asset"
    version = figureArtifactVersion()

    repositories {
        figureNexusMirrorRepository(project)
        mavenCentral()
    }
}

subprojects {
    apply {
        Plugins.Kotlin.addTo(this)
        Plugins.Idea.addTo(this)
        plugin("java")
        plugin("maven-publish")
    }

    repositories {
        figureNexusMirrorRepository(project)
        mavenCentral()
        flatDir {
            dirs("${project.projectDir}/../lib")
        }
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

publishing {
    repositories {
        figureNexusFigureRepository(project)
    }
}
