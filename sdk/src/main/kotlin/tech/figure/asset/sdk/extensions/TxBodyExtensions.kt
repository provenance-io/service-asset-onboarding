package tech.figure.asset.sdk.extensions

import com.google.protobuf.util.JsonFormat
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest

fun TxOuterClass.TxBody.toJson(): String {
    val printer: JsonFormat.Printer = JsonFormat.printer().usingTypeRegistry(
        JsonFormat.TypeRegistry.newBuilder()
            .add(MsgWriteRecordRequest.getDescriptor())
            .add(MsgWriteScopeRequest.getDescriptor())
            .build()
    )
    return printer.print(this)
}
