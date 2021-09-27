package tech.figure.asset.extensions

import tech.figure.asset.OBJECT_MAPPER
import java.io.File
import java.util.*

fun writeFile(fileName: String, text: String) = File(fileName).writeText(text, Charsets.UTF_8)
fun readFileAsText(fileName: String): String = File(fileName).readText(Charsets.UTF_8)
fun randomUuid() = UUID.randomUUID().toString()
fun <T> T.toJsonString() = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this)

