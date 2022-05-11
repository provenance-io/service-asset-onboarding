plugins {
    `java-library`
    `maven-publish`
    signing
}

dependencies {

    api("io.envoyproxy.protoc-gen-validate:pgv-java-stub:0.6.2")
    api("com.google.protobuf:protobuf-java:${Versions.Protobuf}")

    listOf(
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
        Dependencies.Provenance.Protobuf.PbProtoKotlin
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

java {
    withSourcesJar()
    withJavadocJar()
}

// Pre-declare artifact name because the create context will swap the name to "maven"
val artifactName = name

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = artifactName
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Asset Onboarding SDK")
                description.set("Tooling for creating Asset scopes and storing in Object Store")
                url.set("https://provenance.io")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("vwagner")
                        name.set("Valerie Wagner")
                        email.set("vwagner@figure.com")
                    }
                    developer {
                        id.set("kherzinger-figure")
                        name.set("Kory Herzinger")
                        email.set("kherzinger@figure.com")
                    }
                    developer {
                        id.set("jazzy-figure")
                        name.set("Jahanzeb Baber")
                        email.set("jbaber@figure.com")
                    }
                    developer {
                        id.set("afremuth-figure")
                        name.set("Anthony Fremuth")
                        email.set("afremuth@figure.com")
                    }
                    developer {
                        id.set("johnlouiefigure")
                        name.set("John Louie")
                        email.set("jlouie@figure.com")
                    }
                }
                scm {
                    developerConnection.set("git@github.com:provenance.io/service-asset-onboarding.git")
                    connection.set("https://github.com/provenance-io/service-asset-onboarding.git")
                    url.set("https://github.com/provenance-io/service-asset-onboarding")
                }
            }
        }
    }

    configure<SigningExtension> {
        sign(publications["maven"])
    }
}
