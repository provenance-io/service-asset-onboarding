import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.kotlin.dsl.exclude
import org.gradle.plugin.use.PluginDependenciesSpec

object RepositoryLocations {
    const val FigureNexusMirror = "https://nexus.figure.com/repository/mirror"
    const val FigureNexusFigure = "https://nexus.figure.com/repository/figure"
}

object Versions {
    // Branch targets for internal deps
    const val Master = "master-+"
    const val Develop = "develop-+"

    const val ProvenanceCore = Master
    const val ProvenancePbc = Master
    const val ProvenanceProtobuf = Master
    const val P8eScope = "0.1.0"
    const val StreamData = Master
    const val WalletPbClient = Develop
    // const val Detekt = "1.17.0"
    const val Kotlin = "1.5.0"
    const val KotlinCoroutines = "1.5.0"
    const val KotlinXCli = "0.3.3"
    const val Protobuf = "3.16.0"
    const val SpringBoot = "2.4.5"
    const val KotlinLogging = "2.0.6"
    const val Retrofit = "2.9.0"
    const val OkHttp = "4.2.1"
    const val Arrow = "0.13.2"
    const val Reactor = "3.4.6"
    const val JunitJupiter = "5.2.0"
    const val Guava = "30.1.1-jre"
    const val Swagger = "1.6.2"
    const val BouncyCastle = "1.69"
}

object Plugins { // please keep this sorted in sections
    // Kotlin
    val Kotlin = PluginSpec("kotlin", Versions.Kotlin)

    // 3rd Party
    // val Detekt = PluginSpec("io.gitlab.arturbosch.detekt", Versions.Detekt)
    val Flyway = PluginSpec("org.flywaydb.flyway", "7.7.0")
    val Idea = PluginSpec("idea")
    val Protobuf = PluginSpec("com.google.protobuf", "0.8.16")
    val SpringBoot = PluginSpec("org.springframework.boot", Versions.SpringBoot)
    val SpringDependencyManagement = PluginSpec("io.spring.dependency-management", "1.0.11.RELEASE")
}

object Dependencies {
    // Kotlin
    object Kotlin {
        val AllOpen = DependencySpec("org.jetbrains.kotlin:kotlin-allopen", Versions.Kotlin)
        val Reflect = DependencySpec("org.jetbrains.kotlin:kotlin-reflect", Versions.Kotlin)
        val StdlbJdk8 = DependencySpec("org.jetbrains.kotlin:kotlin-stdlib-jdk8", Versions.Kotlin)
        val CoroutinesCoreJvm = DependencySpec(
            "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm",
            Versions.KotlinCoroutines
        )
        val CoroutinesReactor = DependencySpec(
            "org.jetbrains.kotlinx:kotlinx-coroutines-reactor",
            Versions.KotlinCoroutines
        )
        val CoroutinesJdk8 = DependencySpec(
            "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8",
            Versions.KotlinCoroutines
        )
        val CoroutinesTest = DependencySpec(
            "org.jetbrains.kotlinx:kotlinx-coroutines-test",
            Versions.KotlinCoroutines
        )
    }

    object KotlinX {
        val CLI = DependencySpec("org.jetbrains.kotlinx:kotlinx-cli", Versions.KotlinXCli)
    }

    // Spring Boot
    object SpringBoot {
        val Starter = DependencySpec("org.springframework.boot:spring-boot-starter")
        val StarterWeb = DependencySpec(
            name = "org.springframework.boot:spring-boot-starter-web",
            exclude = listOf("org.springframework.boot:spring-boot-starter-tomcat")
        )
        val StarterWebFlux = DependencySpec(
            name = "org.springframework.boot:spring-boot-starter-webflux",
            exclude = listOf("org.springframework.boot:spring-boot-starter-tomcat")
        )
        val StarterJetty = DependencySpec("org.springframework.boot:spring-boot-starter-jetty")
        val StarterActuator = DependencySpec("org.springframework.boot:spring-boot-starter-actuator")
        val StarterDevTools = DependencySpec("org.springframework.boot:spring-boot-devtools")
        val StarterSecurity = DependencySpec("org.springframework.boot:spring-boot-starter-security")
        val StarterValidation = DependencySpec("org.springframework.boot:spring-boot-starter-validation")

        val StarterTest =
            DependencySpec(
                name = "org.springframework.boot:spring-boot-starter-test",
                exclude = listOf(
                    "org.junit.vintage:junit-vintage-engine",
                    "org.mockito:mockito-core"
                )
            )
    }

    // Arrow
    object Arrow {
        val Core = DependencySpec("io.arrow-kt:arrow-core", Versions.Arrow)
    }

    // Project Reactor
    object Reactor {
        // https://github.com/reactor/reactor-core
        val Core = DependencySpec("io.projectreactor:reactor-core", Versions.Reactor)
    }

    // Protobuf
    object Protobuf {
        val Java = DependencySpec("com.google.protobuf:protobuf-java", Versions.Protobuf)
        val JavaUtil = DependencySpec("com.google.protobuf:protobuf-java-util", Versions.Protobuf)
    }

    // Square's Retrofit API client
    object Retrofit {
        val Core = DependencySpec("com.squareup.retrofit2:retrofit", Versions.Retrofit)
        val JacksonConverter = DependencySpec("com.squareup.retrofit2:converter-jackson", Versions.Retrofit)
        val ScalarsConverter = DependencySpec("com.squareup.retrofit2:converter-scalars", Versions.Retrofit)
    }

    object BouncyCastle {
        val ProvJDK15On = DependencySpec("org.bouncycastle:bcprov-jdk15on", Versions.BouncyCastle)
    }

    object OkHttp {
        val Core = DependencySpec("com.squareup.okhttp3:okhttp", Versions.OkHttp)
        val LoggingInterceptor = DependencySpec("com.squareup.okhttp3:logging-interceptor", Versions.OkHttp)
    }

    object Jackson {
        val KotlinModule = DependencySpec(
            "com.fasterxml.jackson.module:jackson-module-kotlin",
            "2.12.+"
        )
        val Hubspot = DependencySpec(
            "com.hubspot.jackson:jackson-datatype-protobuf",
            "0.9.9-jackson2.9-proto3"
        )
    }

    val GoogleGuava = DependencySpec("com.google.guava:guava", Versions.Guava)

    object Swagger {
        val Annotations = DependencySpec("io.swagger:swagger-annotations", Versions.Swagger)
    }

    // Figure
    object Figure {
        val StreamData = DependencySpec("com.figure:stream-data", Versions.StreamData, isChanging = true)
        object Wallet {
            val PbClient = DependencySpec("com.figure.wallet:pb-client", Versions.WalletPbClient)
        }
    }

    object Provenance {
        val CoreLocking = DependencySpec("io.provenance:core-locking", Versions.ProvenanceCore)
        val CoreLogging = DependencySpec("io.provenance:core-logging", Versions.ProvenanceCore)
        val CoreCoroutinesSupport = DependencySpec(
            "io.provenance:core-coroutines-support",
            Versions.ProvenanceCore
        )
        val PbcProto = DependencySpec("io.provenance.pbc:pbc-proto", Versions.ProvenancePbc)
        object Protobuf {
            val PbProtoJava = DependencySpec("io.provenance.protobuf:pb-proto-java", Versions.ProvenanceProtobuf)
        }
    }

    object P8eScope {
        val Encryption = DependencySpec("io.provenance.scope:encryption", Versions.P8eScope)
        val OsClient = DependencySpec("io.provenance.scope:os-client", Versions.P8eScope)
        val Sdk = DependencySpec("io.provenance.scope:sdk", Versions.P8eScope)
        val Util = DependencySpec("io.provenance.scope:util", Versions.P8eScope)
    }

    val KotlinLogging = DependencySpec("io.github.microutils:kotlin-logging-jvm", Versions.KotlinLogging)

    val Ktlint = DependencySpec("com.pinterest:ktlint", "0.41.0")
    val Mockk = DependencySpec("io.mockk:mockk", "1.11.0")
    val Hamkrest = DependencySpec("com.natpryce:hamkrest", "1.8.0.1")
    val SpringMockk = DependencySpec("com.ninja-squad:springmockk", "3.0.1")
    val KotlinFaker = DependencySpec("io.github.serpro69:kotlin-faker:1.7.1")

    object Jupiter {
        val JupiterApi = DependencySpec("org.junit.jupiter:junit-jupiter-api", Versions.JunitJupiter)
        val JupiterEngine = DependencySpec("org.junit.jupiter:junit-jupiter-engine", Versions.JunitJupiter)
    }
}

data class PluginSpec(
    val id: String,
    val version: String = ""
) {
    fun addTo(scope: PluginDependenciesSpec) {
        scope.also {
            it.id(id).version(version.takeIf { v -> v.isNotEmpty() })
        }
    }

    fun addTo(action: ObjectConfigurationAction) {
        action.plugin(this.id)
    }
}

data class DependencySpec(
    val name: String,
    val version: String = "",
    val isChanging: Boolean = false,
    val exclude: List<String> = emptyList()
) {
    fun plugin(scope: PluginDependenciesSpec) {
        scope.apply {
            id(name).version(version.takeIf { it.isNotEmpty() })
        }
    }

    fun classpath(scope: ScriptHandlerScope) {
        val spec = this
        with(scope) {
            dependencies {
                classpath(spec.toDependencyNotation())
            }
        }
    }

    fun implementation(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "implementation".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                spec.exclude.forEach { excludeDependencyNotation ->
                    val (group, module) = excludeDependencyNotation.split(":", limit = 2)
                    this.exclude(group = group, module = module)
                }
            }
        }
    }

    fun testImplementation(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "testImplementation".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                spec.exclude.forEach { excludeDependencyNotation ->
                    val (group, module) = excludeDependencyNotation.split(":", limit = 2)
                    this.exclude(group = group, module = module)
                }
            }
        }
    }

    fun testRuntimeOnly(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "testRuntimeOnly".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                spec.exclude.forEach { excludeDependencyNotation ->
                    val (group, module) = excludeDependencyNotation.split(":", limit = 2)
                    this.exclude(group = group, module = module)
                }
            }
        }
    }

    fun toDependencyNotation(): String =
        listOfNotNull(
            name,
            version.takeIf { it.isNotEmpty() }
        ).joinToString(":")
}
