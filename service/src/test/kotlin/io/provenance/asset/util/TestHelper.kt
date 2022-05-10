package io.provenance.asset.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.protobuf.AbstractMessage.Builder
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import java.io.File
import java.nio.file.Paths

const val TEST_DATA_FOLDER = "TestData/"

object TestHelper {

    fun getDataFromFile(fileName: String): String =
        File(
            Paths.get(
                this.javaClass.classLoader.getResource("$TEST_DATA_FOLDER$fileName").toURI()
            ).toString()
        ).readText(Charsets.UTF_8)

    inline fun <reified T : Message> getTestDataToProtobuf(fileName: String, clazz: Class<T>): List<T> =
        jacksonObjectMapper().readTree(
            getDataFromFile(fileName)
        ).map { jsonNode ->
            (clazz.getMethod("newBuilder").invoke(null) as Builder<*>)
                .also { builder ->
                    JsonFormat.parser().merge(
                        jacksonObjectMapper().writeValueAsString(jsonNode),
                        builder
                    )
                }.build() as T
        }
}
