plugins {
    `java-library`
}

dependencies {
    listOf(
        Dependencies.Kotlin.StdlbJdk8,
        Dependencies.Kotlin.CoroutinesCoreJvm,
        Dependencies.Kotlin.CoroutinesJdk8
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
