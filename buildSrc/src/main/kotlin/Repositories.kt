import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

fun Project.figureNexusUsername(): String? {
    return findProperty("nexusUser")?.toString() ?: System.getenv("NEXUS_USER")
}

fun Project.figureNexusPassword(): String? {
    return findProperty("nexusPass")?.toString() ?: System.getenv("NEXUS_PASS")
}

fun RepositoryHandler.mavenRepository(project: Project, repoUrl: String): MavenArtifactRepository = maven {
    url = project.uri(repoUrl)
    credentials {
        username = project.figureNexusUsername()
        password = project.figureNexusPassword()
    }
}

// TODO: Remove before transferring to public repository - kept for figure nexus publishing
fun RepositoryHandler.figureNexusFigureRepository(project: Project): MavenArtifactRepository =
    mavenRepository(project, "https://nexus.figure.com/repository/figure")
