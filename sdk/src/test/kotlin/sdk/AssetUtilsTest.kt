package tech.figure.asset.sdk

import io.provenance.metadata.v1.DefinitionType
import io.provenance.metadata.v1.MsgWriteContractSpecificationRequest
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteRecordSpecificationRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteScopeSpecificationRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.metadata.v1.ResultStatus
import io.provenance.scope.encryption.dime.ProvenanceDIME
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.encryption.model.DirectKeyRef
import io.provenance.scope.encryption.proto.Encryption
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.util.MetadataAddress
import java.security.KeyPair
import java.util.UUID
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.figure.asset.Asset
import java.io.ByteArrayInputStream

class AssetUtilsTest {

    companion object {
        val OS_CONFIG_URL: String = "grpc://localhost:8081"
        val OS_CONFIG_TIMEOUT_MS: Long = 20000

        val ASSET_TYPE: String = "TestAssetType"
        val ASSET_NAME: String = "TestAssetName"

        val ASSET_RECORD_NAME: String = "TestRecordName"
        val ASSET_RECORD_INPUT_1_NAME: String = "TestInputName"
        val ASSET_RECORD_INPUT_1_HASH: String = "TestInputHash"

        val ENCRYPTED_ASSET_SIZE: Int = 102
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
    fun `#getDIME returns DIME object for asset`() {
        runBlockingTest {
            assetUtils.encryptAndStore(testAsset, testKeyPair.public).also { hash ->
                val dime: Encryption.DIME = assetUtils.getDIME(hash, testKeyPair.public)

                Assertions.assertNotNull(dime.uuid.value)
                Assertions.assertDoesNotThrow { UUID.fromString(dime.uuid.value) }
            }
        }
    }

    @Test
    fun `#retrieve returns encrypted asset`() {
        runBlockingTest {
            assetUtils.encryptAndStore(testAsset, testKeyPair.public).also { hash ->
                val encrypted = assetUtils.retrieve(hash, testKeyPair.public)

                Assertions.assertEquals(encrypted.size, ENCRYPTED_ASSET_SIZE)
            }
        }
    }

    @Test
    fun `#retrieveWithDIME returns encrypted asset`() {
        runBlockingTest {
            assetUtils.encryptAndStore(testAsset, testKeyPair.public).also { hash ->
                assetUtils.retrieveWithDIME(hash, testKeyPair.public).also { result ->
                    val dime = result.first
                    val encrypted = result.second
                    val decrypted = ProvenanceDIME.getDEK(dime.audienceList, DirectKeyRef(testKeyPair.public, testKeyPair.private))
                        .let { ProvenanceDIME.decryptPayload(ByteArrayInputStream(encrypted), it) }
                    val decryptedAsset: Asset = Asset.parseFrom(decrypted)

                    Assertions.assertEquals(testAsset.id, decryptedAsset.id)
                    Assertions.assertEquals(testAsset.type, decryptedAsset.type)
                    Assertions.assertEquals(testAsset.name, decryptedAsset.name)
                }
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
                Assertions.assertEquals(testScopeId, result.first)

                // there should be two messages in the tx body
                Assertions.assertEquals(6, result.second.messagesCount)

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
                var scopeSessionId = ""

                // the first message is the write-contract-specification request
                result.second.getMessages(0).unpack(MsgWriteContractSpecificationRequest::class.java).also { writeContractSpec ->
                    // the contract spec id exists
                    Assertions.assertNotNull(writeContractSpec.specUuid)
                    contractSpecId = writeContractSpec.specUuid

                    // there's only one signer
                    Assertions.assertEquals(1, writeContractSpec.signersCount)
                    writeContractSpec.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }

                    // the contract specification exists
                    writeContractSpec.specification.also { contractSpec ->
                        Assertions.assertArrayEquals(MetadataAddress.forContractSpecification(UUID.fromString(contractSpecId)).bytes, contractSpec.specificationId.toByteArray())
                        Assertions.assertEquals("dummyContractSpecHash", contractSpec.hash)
                        Assertions.assertEquals("dummyContractSpecClassName", contractSpec.className)

                        // there's only one owner for the contract specification
                        Assertions.assertEquals(1, contractSpec.ownerAddressesCount)
                        contractSpec.getOwnerAddresses(0).also { owner ->
                            Assertions.assertEquals(testKeyPair.public.getAddress(false), owner)
                        }

                        // there's only one party involved for the contract specification
                        Assertions.assertEquals(1, contractSpec.partiesInvolvedCount)
                        contractSpec.getPartiesInvolved(0).also { partyType ->
                            Assertions.assertEquals(PartyType.PARTY_TYPE_OWNER, partyType)
                        }
                    }
                }

                // the second message is the write-scope-specification request
                result.second.getMessages(1).unpack(MsgWriteScopeSpecificationRequest::class.java).also { writeScopeSpec ->
                    // the scope spec id exists
                    Assertions.assertNotNull(writeScopeSpec.specUuid)
                    scopeSpecId = writeScopeSpec.specUuid

                    // there's only one signer
                    Assertions.assertEquals(1, writeScopeSpec.signersCount)
                    writeScopeSpec.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }

                    // the scope specification exists
                    writeScopeSpec.specification.also { scopeSpec ->
                        // there's only one contract specification for the scope specification
                        Assertions.assertEquals(1, scopeSpec.contractSpecIdsCount)
                        scopeSpec.getContractSpecIds(0).also { scopeContractSpecId ->
                            Assertions.assertEquals(UUID.fromString(contractSpecId), MetadataAddress.fromBytes(scopeContractSpecId.toByteArray()).getPrimaryUuid())
                        }

                        // there's only one owner for the scope specification
                        Assertions.assertEquals(1, scopeSpec.ownerAddressesCount)
                        scopeSpec.getOwnerAddresses(0).also { owner ->
                            Assertions.assertEquals(testKeyPair.public.getAddress(false), owner)
                        }

                        // there's only one party involved for the scope specification
                        Assertions.assertEquals(1, scopeSpec.partiesInvolvedCount)
                        scopeSpec.getPartiesInvolved(0).also { partyType ->
                            Assertions.assertEquals(PartyType.PARTY_TYPE_OWNER, partyType)
                        }
                    }
                }

                // the third message is the write-scope request
                result.second.getMessages(2).unpack(MsgWriteScopeRequest::class.java).also { writeScope ->
                    // the scope id is set
                    Assertions.assertEquals(testScopeId, UUID.fromString(writeScope.scopeUuid))

                    // the scope spec id is set
                    Assertions.assertEquals(UUID.fromString(scopeSpecId), UUID.fromString(writeScope.specUuid))

                    // there's only one signer
                    Assertions.assertEquals(1, writeScope.signersCount)
                    writeScope.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }

                    // the scope exists
                    writeScope.scope.also { scope ->
                        // the value owner of the scope is set
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), scope.valueOwnerAddress)

                        // there's only one owner
                        Assertions.assertEquals(1, scope.ownersCount)
                        scope.getOwners(0).also { owner ->
                            Assertions.assertEquals(testKeyPair.public.getAddress(false), owner.address)
                            Assertions.assertEquals(PartyType.PARTY_TYPE_OWNER, owner.role)
                        }
                    }
                }

                // the fourth message is the write-session request
                result.second.getMessages(3).unpack(MsgWriteSessionRequest::class.java).also { writeSession ->
                    // the session id is valid
                    writeSession.sessionIdComponents.also { sessionIdComponents ->
                        // the scope id is set
                        Assertions.assertEquals(testScopeId, UUID.fromString(sessionIdComponents.scopeUuid))

                        // the session id exists
                        Assertions.assertNotNull(sessionIdComponents.sessionUuid)
                        scopeSessionId = sessionIdComponents.sessionUuid
                    }

                    // there's only one signer
                    Assertions.assertEquals(1, writeSession.signersCount)
                    writeSession.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }

                    // the session exists
                    writeSession.session.also { session ->
                        // the session id is set
                        Assertions.assertArrayEquals(MetadataAddress.forSession(testScopeId, UUID.fromString(scopeSessionId)).bytes, session.sessionId.toByteArray())

                        // the specification id is set
                        Assertions.assertArrayEquals(MetadataAddress.forContractSpecification(UUID.fromString(contractSpecId)).bytes, session.specificationId.toByteArray())

                        // there's only one party involved for the session
                        Assertions.assertEquals(1, session.partiesCount)
                        session.getParties(0).also { party ->
                            Assertions.assertEquals(testKeyPair.public.getAddress(false), party.address)
                            Assertions.assertEquals(PartyType.PARTY_TYPE_OWNER, party.role)
                        }

                        // the audit is set
                        session.audit.also { audit ->
                            Assertions.assertEquals(testKeyPair.public.getAddress(false), audit.createdBy)
                            Assertions.assertEquals(testKeyPair.public.getAddress(false), audit.updatedBy)
                            Assertions.assertEquals(1, audit.version)
                        }
                    }
                }

                // the fifth message is the write-record-specification request
                result.second.getMessages(4).unpack(MsgWriteRecordSpecificationRequest::class.java).also { writeRecordSpec ->
                    // the contract spec id is set
                    Assertions.assertEquals(contractSpecId, writeRecordSpec.contractSpecUuid)

                    // there's only one signer
                    Assertions.assertEquals(1, writeRecordSpec.signersCount)
                    writeRecordSpec.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }

                    // the specification exists
                    writeRecordSpec.specification.also { recordSpec ->
                        // the record name is set
                        Assertions.assertEquals(ASSET_RECORD_NAME, recordSpec.name)

                        // the specification id is valid
                        Assertions.assertArrayEquals(MetadataAddress.forRecordSpecification(UUID.fromString(contractSpecId), ASSET_RECORD_NAME).bytes, recordSpec.specificationId.toByteArray())

                        // the type name is set
                        Assertions.assertEquals("${ASSET_RECORD_NAME}Type", recordSpec.typeName)

                        // the result type is set
                        Assertions.assertEquals(DefinitionType.DEFINITION_TYPE_RECORD, recordSpec.resultType)

                        // there's only one party involved for the record specification
                        Assertions.assertEquals(recordSpec.responsiblePartiesCount, 1)
                        recordSpec.getResponsibleParties(0).also { partyType ->
                            Assertions.assertEquals(PartyType.PARTY_TYPE_OWNER, partyType)
                        }

                        // there's only one input
                        Assertions.assertEquals(recordSpec.inputsCount, 1)
                        recordSpec.getInputs(0).also { input ->
                            Assertions.assertEquals(ASSET_RECORD_INPUT_1_NAME, input.name)
                            Assertions.assertEquals("${ASSET_RECORD_INPUT_1_NAME}Type", input.typeName)
                            Assertions.assertEquals("dummyRecordSpecHash", input.hash)
                        }
                    }
                }

                // the sixth message is the write-record request
                result.second.getMessages(5).unpack(MsgWriteRecordRequest::class.java).also { writeRecord ->
                    // the contract spec id is set
                    Assertions.assertEquals(contractSpecId, writeRecord.contractSpecUuid)

                    // there's only one signer
                    Assertions.assertEquals(1, writeRecord.signersCount)
                    writeRecord.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }

                    // there's only one party
                    Assertions.assertEquals(1, writeRecord.partiesCount)
                    writeRecord.getParties(0).also { party ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), party.address)
                        Assertions.assertEquals(PartyType.PARTY_TYPE_OWNER, party.role)
                    }

                    // the record is set
                    writeRecord.record.also { record ->
                        // the record session id is set
                        Assertions.assertArrayEquals(MetadataAddress.forSession(testScopeId, UUID.fromString(scopeSessionId)).bytes, record.sessionId.toByteArray())

                        // the record has a name
                        Assertions.assertEquals(ASSET_RECORD_NAME, record.name)

                        // the process is set
                        record.process.also { process ->
                            Assertions.assertEquals("dummyProcessName", process.name)
                            Assertions.assertEquals("dummyProcessHash", process.hash)
                            Assertions.assertEquals("dummyProcessMethod", process.method)
                        }

                        // there's only one input in the record
                        Assertions.assertEquals(1, record.inputsCount)
                        record.getInputs(0).also { input ->
                            Assertions.assertEquals(ASSET_RECORD_INPUT_1_NAME, input.name)
                            Assertions.assertEquals("${ASSET_RECORD_INPUT_1_NAME}Type", input.typeName)
                            Assertions.assertEquals(ASSET_RECORD_INPUT_1_HASH, input.hash)
                            Assertions.assertEquals(RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED, input.status)
                        }

                        // there's only one output in the record
                        Assertions.assertEquals(1, record.outputsCount)
                        record.getOutputs(0).also { output ->
                            Assertions.assertEquals(ASSET_RECORD_INPUT_1_HASH, output.hash)
                            Assertions.assertEquals(ResultStatus.RESULT_STATUS_PASS, output.status)
                        }
                    }
                }
            }
        }
    }

}
