import org.gradle.api.Project

fun Project.deriveArtifactVersion(): String = this.findProperty("version")?.toString() ?: "1.0-SNAPSHOT"
