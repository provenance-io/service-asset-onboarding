import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("java")
    Plugins.Protobuf.addTo(this)
}

dependencies {
    listOf(
        Dependencies.Protobuf.Java,
        Dependencies.Protobuf.JavaUtil,
        Dependencies.Figure.StreamData
    ).forEach { dep ->
        dep.implementation(this)
    }
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
    generateProtoTasks {
        all().forEach {
            it.plugins { ofSourceSet("main") }
        }
    }
}
