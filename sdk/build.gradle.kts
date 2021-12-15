plugins {
    `java-library`
    `maven-publish`
}

dependencies {

    listOf(
        Dependencies.Figure.Wallet.PbClient,
        Dependencies.GoogleGuava,
        Dependencies.Kotlin.StdlbJdk8,
        Dependencies.Kotlin.CoroutinesCoreJvm,
        Dependencies.Kotlin.CoroutinesJdk8,
        Dependencies.Kotlin.Reflect,
        Dependencies.P8eScope.Encryption,
        Dependencies.P8eScope.OsClient,
        Dependencies.P8eScope.Util,
        Dependencies.Protobuf.Java,
        Dependencies.Protobuf.JavaUtil,
        Dependencies.Provenance.AssetModel,
        Dependencies.Provenance.PbcProto,
        Dependencies.Provenance.Protobuf.PbProtoJava
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
    dependsOn("testComposeUp")
    finalizedBy("testComposeDown")
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<Exec>("testComposeUp") {
    workingDir("../")
    commandLine("./dc.sh", "up")
}

tasks.register<Exec>("testComposeDown") {
    workingDir("../")
    commandLine("./dc.sh", "down")
}

publishing {
    repositories {
        figureNexusFigureRepository(project)
    }
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.name.toString()
            version = project.version.toString()

            from(components["java"])
        }
    }
}
