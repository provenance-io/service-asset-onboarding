package tech.figure.asset.util

import com.github.kittinunf.fuel.httpGet
import com.google.protobuf.ByteString
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.TestPropertySource
import tech.figure.asset.Application
import java.io.File

@SpringBootTest(classes = [(Application::class)], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = ["classpath:application-development.properties"])
@Profile("development")
class GenerateAPI {


    @Test
    fun grabApi() {
        println("Grabbing the API...")

        "http://localhost:8080/v3/api-docs".httpGet().response().also {
            it.third.fold(
                success = { response ->
                    val apiJson = ByteString.copyFrom(response).toStringUtf8()
                    println("success: $apiJson") // this is the api json
                    writeFile("generated-api.json", apiJson)
                },
                failure = { println("failure: $it") }
            )
        }
    }
}

fun writeFile(fileName: String, text: String) = File(fileName).writeText(text, Charsets.UTF_8)
fun readFileAsText(fileName: String): String = File(fileName).readText(Charsets.UTF_8)