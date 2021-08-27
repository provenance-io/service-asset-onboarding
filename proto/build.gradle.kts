import com.google.protobuf.gradle.*

plugins {
    id("java")
    Plugins.Protobuf.addTo(this)
    Plugins.Kroto.addTo(this)
}


dependencies {
    listOf(
            Dependencies.Protobuf.Java,
            Dependencies.Protobuf.JavaUtil,
            Dependencies.Protobuf.Kroto,
            Dependencies.Protobuf.JavaAnnotation,
            Dependencies.Protobuf.ProtoValidation,
    ).forEach { dep ->
        dep.implementation(this)
    }
    implementation("io.envoyproxy.protoc-gen-validate:pgv-java-stub:0.6.1")
}

protobuf {

    // protoc plugin names
    val kroto = "kroto"
    val validation = "javapgv"

    protoc {
        if (Platform.OS.isAppleSilicon) {
            // Need to use locally installed protoc on Apple Silicon until maven repo starts serving ARM binaries
            // If not installed: brew install protobuf
            path = "/opt/homebrew/bin/protoc"
        } else {
            artifact = "${Plugins.Protobuf.id}:protoc:${Versions.Protobuf}"
        }
    }
    plugins {
        id(kroto) {
            artifact = Dependencies.Protobuf.Kroto.toDependencyNotation()
        }
        id(validation) {
            artifact = Dependencies.Protobuf.ProtoValidation.toDependencyNotation()
        }
    }
    generateProtoTasks {
        val krotoConfig = file("kroto-config.yaml")

        all().forEach {
            it.inputs.files(krotoConfig)
            it.plugins {
                ofSourceSet("main")
                id(kroto) {
                    outputSubDir = "java"
                    option("ConfigPath=$krotoConfig")
                }
                id(validation) {
                    option("lang=java")
                }
            }
        }
    }
}