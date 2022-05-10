package io.provenance.asset.sdk.extensions

import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream

fun DIMEInputStream.getEncryptedPayload(): ByteArray {
    // seek past the header
    val header = DIMEInputStream::class.java.getDeclaredField("header").let {
        it.isAccessible = true
        it.get(this) as ByteArray
    }
    DIMEInputStream::class.java.getDeclaredField("pos").let {
        it.isAccessible = true
        it.setInt(this, header.size)
    }

    return readAllBytes()
}
