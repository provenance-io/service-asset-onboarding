import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.IncludeResourceTransformer

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
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

val collectDeps = task("collectDeps", Copy::class) {
    from(configurations.default) {
        include("bcp*-jdk15on-1.68.jar")
    }.into("$buildDir/libs")
}

tasks {
    named<ShadowJar>("shadowJar") {
        dependsOn(collectDeps)
        archiveBaseName.set("cli")
        isZip64 = true
        mergeServiceFiles()
        dependencies {
            exclude(dependency("org.bouncycastle::"))
        }
        manifest {
            attributes(mapOf(
                "Class-Path" to "tech.figure.asset.cli lib/bcprov-jdk15on-1.68.jar",
                "Main-Class" to "tech.figure.asset.cli.ApplicationKt"
            ))
        }
        transform(IncludeResourceTransformer::class.java) {
            file = File("$buildDir/libs/bcprov-jdk15on-1.68.jar")
            resource = "lib/bcprov-jdk15on-1.68.jar"
        }
        transform(IncludeResourceTransformer::class.java) {
            file = File("$buildDir/libs/bcpkix-jdk15on-1.68.jar")
            resource = "lib/bcpkix-jdk15on-1.68.jar"
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
