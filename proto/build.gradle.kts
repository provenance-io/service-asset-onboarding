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
    ).forEach { dep ->
        dep.implementation(this)
    }

    implementation("com.github.marcoferrer.krotoplus:protoc-gen-kroto-plus:0.6.1")
    implementation("javax.annotation:javax.annotation-api:1.3.1")

}

protobuf {
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
        id("kroto") {
            artifact = "com.github.marcoferrer.krotoplus:protoc-gen-kroto-plus:0.6.1"
        }
    }
    generateProtoTasks {
        val krotoConfig = file("kroto-config.yaml")

        all().forEach {
            it.inputs.files(krotoConfig)
            it.plugins {
                ofSourceSet("main")
                id("kroto") {
                    outputSubDir = "java"
                    option("ConfigPath=$krotoConfig")
                }
            }
        }
    }
}