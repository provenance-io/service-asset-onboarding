package io.provenance.asset.sdk.extensions

import com.google.protobuf.util.JsonFormat
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.metadata.v1.MsgWriteContractSpecificationRequest
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteRecordSpecificationRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteScopeSpecificationRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest

fun TxOuterClass.TxBody.toJson(): String {
    val printer: JsonFormat.Printer = JsonFormat.printer().usingTypeRegistry(
        JsonFormat.TypeRegistry.newBuilder()
            .add(MsgWriteContractSpecificationRequest.getDescriptor())
            .add(MsgWriteScopeSpecificationRequest.getDescriptor())
            .add(MsgWriteScopeRequest.getDescriptor())
            .add(MsgWriteSessionRequest.getDescriptor())
            .add(MsgWriteRecordSpecificationRequest.getDescriptor())
            .add(MsgWriteRecordRequest.getDescriptor())
            .build()
    )
    return printer.print(this)
}
