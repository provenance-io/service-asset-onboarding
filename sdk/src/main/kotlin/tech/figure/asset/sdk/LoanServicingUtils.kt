package tech.figure.asset.sdk

import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
import io.provenance.metadata.v1.*
import io.provenance.scope.util.MetadataAddress
import io.provenance.scope.util.toByteString

class LoanServicingUtils(   
    val config: AssetUtilsConfig,
) {
    companion object {
        // Contract specification
        const val ContractSpecClassName = "tech.figure.asset.loan.LoanPoolState"
        const val ContractSpecSourceHash = "DE2479B56149B51A61A7A6E46BE5CD3414820F15A68CAA2333BDC2587E293388" // sha356(ContractSpecClassName)

        // Record specification
        const val RecordSpecName = "LoanPoolState"
        const val RecordSpecTypeName = "tech.figure.asset.loan.LoanPoolState"
        val RecordSpecInputs = listOf(RecordInputSpec(
            name = "LoanPoolState",
            typeName = "String",
            hash = "CD0308DFFDBF1B9FF844375437981F925F9112730B7BF87C7D6CC701EBFB8F0B", // sha356(RecordSpecInputs.name)
        ))

        // Record process
        const val RecordProcessName = "LoanPoolStateProcess"
        const val RecordProcessMethod = "LoanPoolState"
        const val RecordProcessHash = "CD0308DFFDBF1B9FF844375437981F925F9112730B7BF87C7D6CC701EBFB8F0B" // sha356(RecordProcessMethod)
    }


    // Builds the Provenance metadata transaction for writing contract/scope/record specifications to the chain
    fun buildAssetSpecificationMetadataTransaction(owner: String): TxOuterClass.TxBody {
        return listOf(

            // write-contract-specification
            MsgWriteContractSpecificationRequest.newBuilder().apply {
                specificationBuilder
                    .setSpecificationId(MetadataAddress.forContractSpecification(config.specConfig.contractSpecId).bytes.toByteString())
                    .setClassName(ContractSpecClassName)
                    .setHash(ContractSpecSourceHash)
                    .addAllOwnerAddresses(listOf(owner))
                    .addAllPartiesInvolved(listOf(
                        PartyType.PARTY_TYPE_OWNER
                    ))
            }.addAllSigners(listOf(owner)).build().toAny(),

            // write-scope-specification
            MsgWriteScopeSpecificationRequest.newBuilder().apply {
                specificationBuilder
                    .setSpecificationId(MetadataAddress.forScopeSpecification(config.specConfig.scopeSpecId).bytes.toByteString())
                    .addAllContractSpecIds(listOf(
                        MetadataAddress.forContractSpecification(config.specConfig.contractSpecId).bytes.toByteString()
                    ))
                    .addAllOwnerAddresses(listOf(owner))
                    .addAllPartiesInvolved(listOf(
                        PartyType.PARTY_TYPE_OWNER
                    ))
            }.addAllSigners(listOf(owner)).build().toAny(),

            // write-record-specification
            MsgWriteRecordSpecificationRequest.newBuilder().apply {
                specificationBuilder
                    .setName(RecordSpecName)
                    .setTypeName(RecordSpecTypeName)
                    .setSpecificationId(
                        MetadataAddress.forRecordSpecification(config.specConfig.contractSpecId,
                            RecordSpecName
                        ).bytes.toByteString())
                    .setResultType(DefinitionType.DEFINITION_TYPE_RECORD)
                    .addAllResponsibleParties(listOf(
                        PartyType.PARTY_TYPE_OWNER
                    ))
                    .addAllInputs(RecordSpecInputs.map { InputSpecification.newBuilder().apply {
                        name = it.name
                        typeName = it.typeName
                        hash = it.hash
                    }.build() })
            }.addAllSigners(listOf(owner)).build().toAny(),

            ).toTxBody()
    }

}
