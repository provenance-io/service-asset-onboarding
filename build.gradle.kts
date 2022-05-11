plugins {
    kotlin("jvm") version Versions.Kotlin apply false
    id("java")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

allprojects {
    group = "io.provenance.asset"
    version = deriveArtifactVersion()

    println("Project version is: $version")

    repositories {
        mavenCentral()
    }
}

// Enable sonatype gradle commands for maven publication
configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(findProject("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
            password.set(findProject("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
            stagingProfileId.set("3180ca260b82a7") // prevents querying for the staging profile id, performance optimization
        }
    }
}

subprojects {
    apply {
        Plugins.Kotlin.addTo(this)
        Plugins.Idea.addTo(this)
        plugin("java")
    }

    repositories {
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
