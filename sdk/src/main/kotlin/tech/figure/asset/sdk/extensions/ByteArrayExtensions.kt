package tech.figure.asset.sdk.extensions

import java.util.Base64

fun ByteArray.toBase64String(): String =
    Base64.getEncoder().encodeToString(this)

fun ByteArray.decodeBase64ToString(): String =
    Base64.getDecoder().decode(this).toString()

fun String.toBase64(): String =
    Base64.getEncoder().encodeToString(this.toByteArray())

fun String.base64ToByteArray(): ByteArray =
    Base64.getDecoder().decode(this)
