val ktlint: Configuration by configurations.creating

plugins {
    //Plugins.Detekt.addTo(this)
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
        Dependencies.SpringBoot.StarterWebFlux,
        Dependencies.SpringBoot.StarterJetty,
        Dependencies.SpringBoot.StarterDevTools,
        Dependencies.SpringBoot.StarterSecurity,
        Dependencies.SpringBoot.StarterValidation,
        Dependencies.Provenance.CoreLocking,
        Dependencies.Provenance.CoreCoroutinesSupport,
        Dependencies.Retrofit.Core,
        Dependencies.Retrofit.JacksonConverter,
        Dependencies.Retrofit.ScalarsConverter,
        Dependencies.OkHttp.Core,
        Dependencies.OkHttp.LoggingInterceptor,
        // Needed to play nice with WebFlux Coroutines stuff
        Dependencies.Reactor.Core,
        Dependencies.KotlinLogging,
        Dependencies.Jackson.KotlinModule,
        Dependencies.Jackson.Hubspot
    ).forEach { dep ->
        dep.implementation(this)
    }

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    listOf(
        Dependencies.Hamkrest,
        Dependencies.Mockk,
        Dependencies.KotlinFaker,
        Dependencies.Kotlin.CoroutinesTest,
        Dependencies.SpringMockk,
        Dependencies.SpringBoot.StarterTest
    ).forEach { testDep ->
        testDep.testImplementation(this)
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

tasks.register<JavaExec>("ktlint") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args("src/**/*.kt")
}

tasks.named("check") {
    dependsOn(tasks.named("ktlint"))
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


//detekt {
//    toolVersion = Versions.Detekt
//    buildUponDefaultConfig = true
//    config = files("${rootDir.path}/detekt.yml")
//    input = files("src/main/kotlin", "src/test/kotlin")
//}
