package tech.figure.asset.extensions

import com.google.common.io.BaseEncoding
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.ecies.EDUtils.toPublicKey
import tech.figure.asset.exceptions.InvalidPublicKeyException
import java.security.PublicKey

fun String.base64ToPublicKey(): PublicKey = try {
    ECUtils.convertBytesToPublicKey(BaseEncoding.base64().decode(this))
} catch (t: Throwable) {
    try {
        BaseEncoding.base64().decode(this).toPublicKey()
    } catch (t: Throwable) {
        throw InvalidPublicKeyException("Invalid public key", t)
    }
}
