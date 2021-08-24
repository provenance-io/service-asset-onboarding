package tech.figure.asset.sdk.extensions

import java.util.Base64

fun ByteArray.toBase64(): String =
    Base64.getEncoder().encodeToString(this)

fun String.toBase64(): String =
    Base64.getEncoder().encodeToString(this.toByteArray())

fun String.base64ToByteArray(): ByteArray =
    Base64.getDecoder().decode(this)
