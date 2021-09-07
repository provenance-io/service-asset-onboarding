package tech.figure.asset.sdk

import io.provenance.metadata.v1.MsgWriteContractSpecificationRequest
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteRecordSpecificationRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteScopeSpecificationRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.util.MetadataAddress
import java.security.KeyPair
import java.util.UUID
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.figure.asset.Asset

class AssetUtilsTest {

    companion object {
        val OS_CONFIG_URL: String = "grpc://localhost:8081"
        val OS_CONFIG_TIMEOUT_MS: Long = 20000

        val ASSET_TYPE: String = "TestAssetType"
        val ASSET_NAME: String = "TestAssetName"

        val ASSET_RECORD_NAME: String = "TestRecordName"
        val ASSET_RECORD_INPUT_1_NAME: String = "TestInputName"
        val ASSET_RECORD_INPUT_1_HASH: String = "TestInputHash"
    }

    val testAsset: Asset = Asset.newBuilder().also { asset ->
        asset.id = tech.figure.util.UUID.newBuilder().also { uuid ->
            uuid.value = UUID.randomUUID().toString()
        }.build()
        asset.type = ASSET_TYPE
        asset.name = ASSET_NAME
    }.build()

    val testScopeId: UUID = UUID.randomUUID()

    val testKeyPair: KeyPair = ProvenanceKeyGenerator.generateKeyPair()

    val assetUtils = AssetUtils(AssetUtilsConfig(
        osConfig = ObjectStoreConfig(
            url = OS_CONFIG_URL,
            timeoutMs = OS_CONFIG_TIMEOUT_MS
        )
    ))

    @Test
    fun `#encryptAndStore (using random signer) asset can be decrypted to buffer`() {
        runBlockingTest {
            assetUtils.encryptAndStore(testAsset, testKeyPair.public).also { hash ->
                val decrypted = assetUtils.retrieveAndDecrypt(hash, testKeyPair.public, testKeyPair.private)
                val decryptedAsset: Asset = Asset.parseFrom(decrypted)

                Assertions.assertEquals(testAsset.id, decryptedAsset.id)
                Assertions.assertEquals(testAsset.type, decryptedAsset.type)
                Assertions.assertEquals(testAsset.name, decryptedAsset.name)
            }
        }
    }

    @Test
    fun `#encryptAndStore (using random signer) asset can be decrypted to type`() {
        runBlockingTest {
            assetUtils.encryptAndStore(testAsset, testKeyPair.public).also { hash ->
                val decryptedAsset: Asset = assetUtils.retrieveAndDecrypt<Asset>(hash, testKeyPair.public, testKeyPair.private)

                Assertions.assertEquals(testAsset.id, decryptedAsset.id)
                Assertions.assertEquals(testAsset.type, decryptedAsset.type)
                Assertions.assertEquals(testAsset.name, decryptedAsset.name)
            }
        }
    }

    @Test
    fun `#buildNewScopeMetadataTransaction`() {
        runBlockingTest {
            assetUtils.buildNewScopeMetadataTransaction(
                testKeyPair.public.getAddress(false),
                ASSET_RECORD_NAME,
                mapOf(Pair(ASSET_RECORD_INPUT_1_NAME, ASSET_RECORD_INPUT_1_HASH)),
                testScopeId
            ).also { result ->
                // returns the scope id
                Assertions.assertEquals(result.first, testScopeId)

                // there should be two messages in the tx body
                Assertions.assertEquals(result.second.messagesCount, 6)

                /*
                0 MsgWriteContractSpecificationRequest
                1 MsgWriteScopeSpecificationRequest
                2 MsgWriteScopeRequest
                3 MsgWriteSessionRequest
                4 MsgWriteRecordSpecificationRequest
                5 MsgWriteRecordRequest
                 */

                var contractSpecId = ""
                var scopeSpecId = ""

                // the first message is the write-contract-specification request
                result.second.getMessages(0).unpack(MsgWriteContractSpecificationRequest::class.java).also { writeContractSpec ->
                    // the contract spec id exists
                    Assertions.assertNotNull(writeContractSpec.specUuid)
                    contractSpecId = writeContractSpec.specUuid

                    // there's only one signer
                    Assertions.assertEquals(writeContractSpec.signersCount, 1)
                    writeContractSpec.getSigners(0).also { signer ->
                        Assertions.assertEquals(signer, testKeyPair.public.getAddress(false))
                    }

                    // the contract specification exists
                    writeContractSpec.specification.also { contractSpec ->
                        Assertions.assertArrayEquals(contractSpec.specificationId.toByteArray(), MetadataAddress.forContractSpecification(UUID.fromString(contractSpecId)).bytes)
                        Assertions.assertEquals(contractSpec.hash, "dummyContractSpecHash")
                        Assertions.assertEquals(contractSpec.className, "dummyContractSpecClassName")

                        // there's only one owner for the contract specification
                        Assertions.assertEquals(contractSpec.ownerAddressesCount, 1)
                        contractSpec.getOwnerAddresses(0).also { owner ->
                            Assertions.assertEquals(owner, testKeyPair.public.getAddress(false))
                        }

                        // there's only one party involved for the contract specification
                        Assertions.assertEquals(contractSpec.partiesInvolvedCount, 1)
                        contractSpec.getPartiesInvolved(0).also { partyType ->
                            Assertions.assertEquals(partyType, PartyType.PARTY_TYPE_OWNER)
                        }
                    }
                }

                // the second message is the write-scope-specification request
                result.second.getMessages(1).unpack(MsgWriteScopeSpecificationRequest::class.java).also { writeScopeSpec ->
                    // the scope spec id exists
                    Assertions.assertNotNull(writeScopeSpec.specUuid)
                    scopeSpecId = writeScopeSpec.specUuid

                    // there's only one signer
                    Assertions.assertEquals(writeScopeSpec.signersCount, 1)
                    writeScopeSpec.getSigners(0).also { signer ->
                        Assertions.assertEquals(signer, testKeyPair.public.getAddress(false))
                    }

                    // the scope specification exists
                    writeScopeSpec.specification.also { scopeSpec ->
                        // there's only one contract specification for the scope specification
                        Assertions.assertEquals(scopeSpec.contractSpecIdsCount, 1)
                        scopeSpec.getContractSpecIds(0).also { scopeContractSpecId ->
                            Assertions.assertEquals(MetadataAddress.fromBytes(scopeContractSpecId.toByteArray()).getPrimaryUuid(), UUID.fromString(contractSpecId))
                        }

                        // there's only one owner for the scope specification
                        Assertions.assertEquals(scopeSpec.ownerAddressesCount, 1)
                        scopeSpec.getOwnerAddresses(0).also { owner ->
                            Assertions.assertEquals(owner, testKeyPair.public.getAddress(false))
                        }

                        // there's only one party involved for the scope specification
                        Assertions.assertEquals(scopeSpec.partiesInvolvedCount, 1)
                        scopeSpec.getPartiesInvolved(0).also { partyType ->
                            Assertions.assertEquals(partyType, PartyType.PARTY_TYPE_OWNER)
                        }
                    }
                }

                // the third message is the write-scope request
                result.second.getMessages(2).unpack(MsgWriteScopeRequest::class.java).also { writeScope ->
                    // the scope id is set
                    Assertions.assertEquals(UUID.fromString(writeScope.scopeUuid), testScopeId)

                    // the scope spec id is set
                    Assertions.assertEquals(UUID.fromString(writeScope.specUuid), UUID.fromString(scopeSpecId))

                    // there's only one signer
                    Assertions.assertEquals(writeScope.signersCount, 1)
                    writeScope.getSigners(0).also { signer ->
                        Assertions.assertEquals(signer, testKeyPair.public.getAddress(false))
                    }

                    // the scope exists
                    writeScope.scope.also { scope ->
                        // the value owner of the scope is set
                        Assertions.assertEquals(scope.valueOwnerAddress, testKeyPair.public.getAddress(false))

                        // there's only one owner
                        Assertions.assertEquals(scope.ownersCount, 1)
                        scope.getOwners(0).also { owner ->
                            Assertions.assertEquals(owner.address, testKeyPair.public.getAddress(false))
                            Assertions.assertEquals(owner.role, PartyType.PARTY_TYPE_OWNER)
                        }
                    }
                }

                // the fourth message is the write-session request
                result.second.getMessages(3).unpack(MsgWriteSessionRequest::class.java).also { writeSession ->
                    // TODO
                }

                // the fifth message is the write-record-specification request
                result.second.getMessages(4).unpack(MsgWriteRecordSpecificationRequest::class.java).also { writeRecordSpec ->
                    // TODO
                }

                // the sixth message is the write-record request
                result.second.getMessages(5).unpack(MsgWriteRecordRequest::class.java).also { writeRecord ->
                    // TODO

                    // there's only one signer
                    Assertions.assertEquals(writeRecord.signersCount, 1)
                    writeRecord.getSigners(0).also { signer ->
                        Assertions.assertEquals(signer, testKeyPair.public.getAddress(false))
                    }

                    // there's only one party
                    Assertions.assertEquals(writeRecord.partiesCount, 1)
                    writeRecord.getParties(0).also { party ->
                        Assertions.assertEquals(party.address, testKeyPair.public.getAddress(false))
                        Assertions.assertEquals(party.role, PartyType.PARTY_TYPE_OWNER)
                    }

                    // the record has a name
                    Assertions.assertEquals(writeRecord.record.name, ASSET_RECORD_NAME)

                    // there's only one input in the record
                    Assertions.assertEquals(writeRecord.record.inputsCount, 1)
                    writeRecord.record.getInputs(0).also { input ->
                        Assertions.assertEquals(input.name, ASSET_RECORD_INPUT_1_NAME)
                        Assertions.assertEquals(input.hash, ASSET_RECORD_INPUT_1_HASH)
                        Assertions.assertEquals(input.status, RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED)
                    }
                }
            }
        }
    }

}
