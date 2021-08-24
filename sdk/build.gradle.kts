plugins {
    `java-library`
}

dependencies {
    implementation(project(":proto"))

    listOf(
        Dependencies.Kotlin.StdlbJdk8,
        Dependencies.Kotlin.CoroutinesCoreJvm,
        Dependencies.Kotlin.CoroutinesJdk8,
        Dependencies.Kotlin.Reflect,
        Dependencies.Protobuf.Java,
        Dependencies.GoogleGuava,
        Dependencies.P8eScope.Encryption,
        Dependencies.P8eScope.OsClient
    ).forEach { dep ->
        dep.implementation(this)
    }

    listOf(
        Dependencies.Kotlin.CoroutinesTest,
        Dependencies.Jupiter.JupiterApi
    ).forEach { testDep ->
        testDep.testImplementation(this)
    }

    listOf(
        Dependencies.Jupiter.JupiterEngine
    ).forEach { runtimeDep ->
        runtimeDep.testRuntimeOnly(this)
    }
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}
