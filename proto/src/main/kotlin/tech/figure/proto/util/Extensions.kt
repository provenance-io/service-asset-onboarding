package tech.figure.proto.util

import com.google.protobuf.*
import com.google.protobuf.Any

fun ByteArray.toProtoAny(): Any = Any.pack(
    BytesValue.newBuilder().setValue(ByteString.copyFrom(this)).build()
)

fun String.toProtoAny(): Any = Any.pack(
    StringValue.newBuilder().setValue(this).build()
)

fun Long.toProtoAny(): Any = Any.pack(
    Int64Value.newBuilder().setValue(this).build()
)

fun Message.toProtoAny(): Any = Any.pack(this)