val ktlint: Configuration by configurations.creating

plugins {
    Plugins.SpringBoot.addTo(this)
    Plugins.SpringDependencyManagement.addTo(this)
    kotlin("plugin.spring") version Versions.Kotlin
}

java.sourceCompatibility = JavaVersion.VERSION_11

dependencyManagement {
    applyMavenExclusions(false)
}

dependencies {
    ktlint(Dependencies.Ktlint.toDependencyNotation())

    implementation(project(":proto"))
    implementation(project(":sdk"))

    listOf(
        Dependencies.Kotlin.AllOpen,
        Dependencies.Kotlin.Reflect,
        Dependencies.Kotlin.StdlbJdk8,
        Dependencies.Kotlin.CoroutinesCoreJvm,
        Dependencies.Kotlin.CoroutinesJdk8,
        Dependencies.Kotlin.CoroutinesReactor,
        Dependencies.SpringBoot.Starter,
        Dependencies.SpringBoot.StarterActuator,
        Dependencies.SpringBoot.StarterWeb,
        Dependencies.SpringBoot.StarterJetty,
        Dependencies.SpringBoot.StarterDevTools,
        Dependencies.SpringBoot.StarterSecurity,
        Dependencies.SpringBoot.StarterValidation,
        Dependencies.SpringBoot.Swagger,
        Dependencies.SpringBoot.SwaggerUI,
        Dependencies.Swagger.Annotations,
        Dependencies.OkHttp.LoggingInterceptor,
        Dependencies.KotlinLogging,
        Dependencies.Arrow.Core,
        Dependencies.P8eScope.Encryption,
        Dependencies.Provenance.PbcProto,
        Dependencies.Provenance.Protobuf.PbProtoJava,
        Dependencies.Jackson.KotlinModule,
        Dependencies.Jackson.Hubspot,
        Dependencies.Figure.Util
    ).forEach { dep ->
        dep.implementation(this)
    }

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    listOf(
        Dependencies.Hamkrest,
        Dependencies.Mockk,
        Dependencies.KotlinFaker,
        Dependencies.Kotlin.CoroutinesTest,
        Dependencies.Jupiter.JupiterApi,
        Dependencies.SpringMockk,
        Dependencies.SpringBoot.StarterTest,
        Dependencies.Fuel
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

tasks.register<JavaExec>("ktlint") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args("src/**/*.kt")
}

tasks.named("check") {
//    dependsOn(tasks.named("ktlint"))
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args("-F", "src/**/*.kt")
}

tasks.bootRun {
    args("--spring.profiles.active=development, local")
}
