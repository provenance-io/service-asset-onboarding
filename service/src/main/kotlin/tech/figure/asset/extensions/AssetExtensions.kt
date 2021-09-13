package tech.figure.asset.extensions

import arrow.core.Either
import com.google.protobuf.util.JsonFormat
import tech.figure.asset.Asset
import java.nio.charset.StandardCharsets

fun String.jsonToAsset(): Either<Throwable, Asset> {
    val assetBuilder = Asset.newBuilder()
    return try {
        JsonFormat.parser().merge(this, assetBuilder)
        Either.Right(assetBuilder.build())
    } catch (t: Throwable) {
        Either.Left(t)
    }
}

fun ByteArray.jsonToAsset(): Either<Throwable, Asset> {
    val assetBuilder = Asset.newBuilder()
    return try {
        JsonFormat.parser().merge(String(this, StandardCharsets.UTF_8), assetBuilder)
        Either.Right(assetBuilder.build())
    } catch (t: Throwable) {
        Either.Left(t)
    }
}
