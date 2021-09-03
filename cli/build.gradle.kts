import org.gradle.jvm.tasks.Jar

plugins {
    application
}

dependencies {
    implementation(project(":proto"))
    implementation(project(":sdk"))

    listOf(
        Dependencies.Figure.Wallet.PbClient,
        Dependencies.Kotlin.StdlbJdk8,
        Dependencies.Kotlin.CoroutinesCoreJvm,
        Dependencies.Kotlin.CoroutinesJdk8,
        Dependencies.Kotlin.Reflect,
        Dependencies.KotlinX.CLI,
        Dependencies.KotlinLogging,
        Dependencies.P8eScope.Encryption,
        Dependencies.Protobuf.JavaUtil,
        Dependencies.Provenance.PbcProto,
        Dependencies.Provenance.Protobuf.PbProtoJava
    ).forEach { dep ->
        dep.implementation(this)
    }
}

application {
    mainClassName = "tech.figure.asset.cli.ApplicationKt"
}

val fatJar = task("fatJar", type = Jar::class) {
    dependsOn("distTar")
    dependsOn("distZip")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    baseName = "${project.name}"
    manifest {
        attributes["Class-Path"] = "tech.figure.asset.cli"
        attributes["Main-Class"] = "tech.figure.asset.cli.ApplicationKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
