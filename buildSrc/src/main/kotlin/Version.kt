import org.gradle.api.Project

fun Project.figureArtifactVersion(): String = this.findProperty("artifactVersion")?.toString()
    ?: "1.0-snapshot"
