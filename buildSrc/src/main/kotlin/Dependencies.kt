import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.kotlin.dsl.exclude
import org.gradle.plugin.use.PluginDependenciesSpec

object Versions {
    const val ProvenanceProtobuf = "1.8.0"
    const val ProvenanceClient = "1.0.5"
    const val ProvenanceHdWallet = "0.1.15"
    const val ProvenanceAssetClassification = "1.0.0"
    const val AssetModel = "0.1.2"
    const val P8eScope = "0.1.0"
    // const val Detekt = "1.17.0"
    const val Kotlin = "1.5.0"
    const val KotlinCoroutines = "1.5.0"
    const val KotlinXCli = "0.3.3"
    const val Protobuf = "3.19.1"
    const val KrotoPlus = "0.6.1"
    const val JavaAnnotation = "1.3.1"
    const val ProtoValidation = "0.6.1"
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
    val Idea = PluginSpec("idea")
    val Protobuf = PluginSpec("com.google.protobuf", "0.8.16")
    val Kroto = PluginSpec("com.github.marcoferrer.kroto-plus", Versions.KrotoPlus)
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
        val Swagger = DependencySpec(    "io.springfox:springfox-boot-starter", "3.0.0")
        val SwaggerUI = DependencySpec(    "io.springfox:springfox-swagger-ui", "3.0.0")

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

    object Protobuf {
        val Java = DependencySpec("com.google.protobuf:protobuf-java", Versions.Protobuf)
        val JavaUtil = DependencySpec("com.google.protobuf:protobuf-java-util", Versions.Protobuf)
        val Kroto = DependencySpec("com.github.marcoferrer.krotoplus:protoc-gen-kroto-plus", Versions.KrotoPlus)
        val JavaAnnotation = DependencySpec("javax.annotation:javax.annotation-api", Versions.JavaAnnotation)
        val ProtoValidation = DependencySpec("io.envoyproxy.protoc-gen-validate:protoc-gen-validate", Versions.ProtoValidation)
    }

    object BouncyCastle {
        val ProvJDK15On = DependencySpec("org.bouncycastle:bcprov-jdk15on", Versions.BouncyCastle)
    }

    object OkHttp {
        val Core = DependencySpec("com.squareup.okhttp3:okhttp", Versions.OkHttp)
        val LoggingInterceptor = DependencySpec("com.squareup.okhttp3:logging-interceptor", Versions.OkHttp)
    }

    object Jackson {
        val Databind = DependencySpec(
            "com.fasterxml.jackson.core:jackson-databind",
            "2.12.+"
        )
        val Datatype = DependencySpec(
            "com.fasterxml.jackson.datatype:jackson-datatype-jsr310",
            "2.12.+"
        )
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

    object Provenance {
        val AssetModel = DependencySpec("io.provenance.model:metadata-asset-model", Versions.AssetModel)
        object Protobuf {
            val PbProtoKotlin = DependencySpec("io.provenance:proto-kotlin", Versions.ProvenanceProtobuf)
        }
        object Client {
            val GrpcClientKotlin = DependencySpec("io.provenance.client:pb-grpc-client-kotlin", Versions.ProvenanceClient)
        }
        object HdWallet {
            val HdWallet = DependencySpec("io.provenance.hdwallet:hdwallet", Versions.ProvenanceHdWallet)
            val HdWalletBase58 = DependencySpec("io.provenance.hdwallet:hdwallet-base58", Versions.ProvenanceHdWallet)
            val HdWalletBech32 = DependencySpec("io.provenance.hdwallet:hdwallet-bech32", Versions.ProvenanceHdWallet)
            val HdWalletBip32 = DependencySpec("io.provenance.hdwallet:hdwallet-bip32", Versions.ProvenanceHdWallet)
            val HdWalletBip39 = DependencySpec("io.provenance.hdwallet:hdwallet-bip39", Versions.ProvenanceHdWallet)
            val HdWalletBip44 = DependencySpec("io.provenance.hdwallet:hdwallet-bip44", Versions.ProvenanceHdWallet)
            val HdWalletEc = DependencySpec("io.provenance.hdwallet:hdwallet-ec", Versions.ProvenanceHdWallet)
            val HdWalletSigner = DependencySpec("io.provenance.hdwallet:hdwallet-signer", Versions.ProvenanceHdWallet)
            val HdWalletCommon = DependencySpec("io.provenance.hdwallet:hdwallet-common", Versions.ProvenanceHdWallet)
        }
    }

    // Square's Retrofit API client
    object Retrofit {
        val Core = DependencySpec("com.squareup.retrofit2:retrofit", Versions.Retrofit, exclude = listOf("com.squareup.okhttp3:okhttp"))
        val JacksonConverter = DependencySpec(
            "com.squareup.retrofit2:converter-jackson",
            Versions.Retrofit,
            exclude = listOf(
                "com.fasterxml.jackson.core:jackson-databind",
                "com.squareup.retrofit2:retrofit"
            )
        )
        val ScalarsConverter = DependencySpec("com.squareup.retrofit2:converter-scalars", Versions.Retrofit)
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
    val Fuel = DependencySpec("com.github.kittinunf.fuel:fuel:2.3.1")

    object Jupiter {
        val JupiterApi = DependencySpec("org.junit.jupiter:junit-jupiter-api", Versions.JunitJupiter)
        val JupiterEngine = DependencySpec("org.junit.jupiter:junit-jupiter-engine", Versions.JunitJupiter)
    }

    object AssetClassification {
        val Client = DependencySpec("io.provenance.classification.asset:client", Versions.ProvenanceAssetClassification)
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
