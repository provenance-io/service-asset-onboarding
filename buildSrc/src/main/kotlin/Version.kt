import org.gradle.api.Project

fun Project.deriveArtifactVersion(): String = this.findProperty("artifactVersion")?.toString()
    ?: "1.0-snapshot"
