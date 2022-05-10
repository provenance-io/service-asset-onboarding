package tech.figure.asset.sdk

import io.provenance.metadata.v1.InputSpecification
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.metadata.v1.RecordSpecification
import io.provenance.metadata.v1.ResultStatus
import io.provenance.scope.encryption.dime.ProvenanceDIME
import io.provenance.scope.encryption.ecies.ECUtils
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
import tech.figure.asset.v1beta1.Asset
import tech.figure.asset.v1beta1.AssetOuterClassBuilders.Asset as AssetBuilder
import java.io.ByteArrayInputStream

class AssetUtilsTest {

    companion object {
        val OS_CONFIG_URL: String = "grpc://localhost:8081"
        val OS_CONFIG_TIMEOUT_MS: Long = 20000

        val ASSET_TYPE: String = "TestAssetType"
        val ASSET_NAME: String = "TestAssetName"

        val ENCRYPTED_ASSET_SIZE: Int = 102
    }

    val testContractSpecId: UUID = UUID.randomUUID()
    val testScopeSpecId: UUID = UUID.randomUUID()

    val testAsset = AssetBuilder {
        idBuilder.value = UUID.randomUUID().toString()
        type = ASSET_TYPE
        description = ASSET_NAME
    }

    val testScopeId: UUID = UUID.randomUUID()

    val testKeyPair: KeyPair = ProvenanceKeyGenerator.generateKeyPair()

    val assetUtils = AssetUtils(AssetUtilsConfig(
        osConfig = ObjectStoreConfig(
            url = OS_CONFIG_URL,
            timeoutMs = OS_CONFIG_TIMEOUT_MS,
        ),
        specConfig = SpecificationConfig(
            contractSpecId = testContractSpecId,
            scopeSpecId = testScopeSpecId,
        )
    ))

    @Test
    fun `Generate Keypair`() {
        val keyPair: KeyPair = ProvenanceKeyGenerator.generateKeyPair()
        val publicKey = ECUtils.publicKeyEncoded(keyPair.public)
        println("publicKey=$publicKey")
    }

    @Test
    fun `#encryptAndStore (using random signer) asset can be decrypted to buffer`() {
        runBlockingTest {
            assetUtils.encryptAndStore(testAsset, testKeyPair.public).also { hash ->
                val decrypted = assetUtils.retrieveAndDecrypt(hash, testKeyPair.public, testKeyPair.private)
                val decryptedAsset: Asset = Asset.parseFrom(decrypted)

                Assertions.assertEquals(testAsset.id, decryptedAsset.id)
                Assertions.assertEquals(testAsset.type, decryptedAsset.type)
                Assertions.assertEquals(testAsset.description, decryptedAsset.description)
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
                Assertions.assertEquals(testAsset.description, decryptedAsset.description)
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
                    Assertions.assertEquals(testAsset.description, decryptedAsset.description)
                }
            }
        }
    }

    @ExperimentalStdlibApi
    @Test
    fun `#buildNewScopeMetadataTransaction with no additional audiences`() {
        runBlockingTest {
            val scopeHash = ""
            assetUtils.buildNewScopeMetadataTransaction(
                testScopeId,
                scopeHash,
                testKeyPair.public.getAddress(false)
            ).also { result ->
                // there should be three messages in the tx body
                Assertions.assertEquals(3, result.messagesCount)

                /*
                    0: MsgWriteScopeRequest
                    1: MsgWriteSessionRequest
                    2: MsgWriteRecordRequest
                */

                var scopeSessionId = ""

                // 0: MsgWriteScopeRequest
                result.getMessages(0).unpack(MsgWriteScopeRequest::class.java).also { writeScope ->
                    // the scope id is set
                    Assertions.assertEquals(testScopeId, UUID.fromString(writeScope.scopeUuid))

                    // the scope spec id is set
                    Assertions.assertEquals(testScopeSpecId, UUID.fromString(writeScope.specUuid))

                    // the scope is set
                    writeScope.scope.also { scope ->
                        // ths scope id metadata address is set
                        Assertions.assertArrayEquals(MetadataAddress.forScope(testScopeId).bytes, scope.scopeId.toByteArray())

                        // the scope spec id metadata address is set
                        Assertions.assertArrayEquals(MetadataAddress.forScopeSpecification(testScopeSpecId).bytes, scope.specificationId.toByteArray())

                        // the value owner of the scope is set
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), scope.valueOwnerAddress)

                        // there's only one owner
                        Assertions.assertEquals(1, scope.ownersCount)
                        scope.getOwners(0).also { owner ->
                            Assertions.assertEquals(testKeyPair.public.getAddress(false), owner.address)
                            Assertions.assertEquals(PartyType.PARTY_TYPE_OWNER, owner.role)
                        }
                    }

                    // there's only one signer
                    Assertions.assertEquals(1, writeScope.signersCount)
                    writeScope.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }
                }

                // 1: MsgWriteSessionRequest
                result.getMessages(1).unpack(MsgWriteSessionRequest::class.java).also { writeSession ->
                    writeSession.sessionIdComponents.also { sessionIdComponents ->
                        // the scope id is set
                        Assertions.assertEquals(testScopeId, UUID.fromString(sessionIdComponents.scopeUuid))

                        // the session id exists
                        Assertions.assertNotNull(sessionIdComponents.sessionUuid)
                        scopeSessionId = sessionIdComponents.sessionUuid
                    }

                    // the session is set
                    writeSession.session.also { session ->
                        // the session id is set
                        Assertions.assertArrayEquals(MetadataAddress.forSession(testScopeId, UUID.fromString(scopeSessionId)).bytes, session.sessionId.toByteArray())

                        // the specification id is set
                        Assertions.assertArrayEquals(MetadataAddress.forContractSpecification(testContractSpecId).bytes, session.specificationId.toByteArray())

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
                        }
                    }

                    // there's only one signer
                    Assertions.assertEquals(1, writeSession.signersCount)
                    writeSession.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }
                }

                // 2: MsgWriteRecordRequest
                result.getMessages(2).unpack(MsgWriteRecordRequest::class.java).also { writeRecord ->
                    // the contract spec id is set
                    Assertions.assertEquals(testContractSpecId, UUID.fromString(writeRecord.contractSpecUuid))

                    // the record is set
                    writeRecord.record.also { record ->
                        // the record session id is set
                        Assertions.assertArrayEquals(MetadataAddress.forSession(testScopeId, UUID.fromString(scopeSessionId)).bytes, record.sessionId.toByteArray())

                        Assertions.assertArrayEquals(
                            MetadataAddress.forRecordSpecification(testContractSpecId, AssetUtils.RecordSpecName).bytes,
                            record.specificationId.toByteArray()
                        )

                        // the record has a name
                        Assertions.assertEquals(AssetUtils.RecordSpecName, record.name)

                        // the record has inputs
                        Assertions.assertEquals(AssetUtils.RecordSpecInputs.size, record.inputsCount)
                        AssetUtils.RecordSpecInputs.forEachIndexed { idx, recordSpecInput ->
                            record.getInputs(idx).also { input ->
                                Assertions.assertEquals(recordSpecInput.name, input.name)
                                Assertions.assertEquals(recordSpecInput.typeName, input.typeName)
                                when(recordSpecInput.hash) {
                                    "AssetHash" -> Assertions.assertEquals(scopeHash, input.hash)
                                    else -> Assertions.assertEquals("", input.hash)
                                }
                                Assertions.assertEquals(RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED, input.status)
                            }
                        }

                        // the record has outputs
                        Assertions.assertEquals(AssetUtils.RecordSpecInputs.size, record.outputsCount)
                        AssetUtils.RecordSpecInputs.forEachIndexed { idx, recordSpecInput ->
                            record.getOutputs(idx).also { output ->
                                when(recordSpecInput.hash) {
                                    "AssetHash" -> Assertions.assertEquals(scopeHash, output.hash)
                                    else -> Assertions.assertEquals("", output.hash)
                                }
                                Assertions.assertEquals(ResultStatus.RESULT_STATUS_PASS, output.status)
                            }
                        }

                        // the process is set
                        record.process.also { process ->
                            Assertions.assertEquals(AssetUtils.RecordProcessName, process.name)
                            Assertions.assertEquals(AssetUtils.RecordProcessHash, process.hash)
                            Assertions.assertEquals(AssetUtils.RecordProcessMethod, process.method)
                        }
                    }

                    // there's only one signer
                    Assertions.assertEquals(1, writeRecord.signersCount)
                    writeRecord.getSigners(0).also { signer ->
                        Assertions.assertEquals(testKeyPair.public.getAddress(false), signer)
                    }
                }
            }
        }
    }

    @ExperimentalStdlibApi
    @Test
    fun `#buildNewScopeMetadataTransaction with provided spec addresses`() {
        runBlockingTest {
            val scopeHash = ""
            // Address generated randomly with the provenanced tool and random uuid: 800ea884-c1c6-11ec-a437-d7326db68487
            val specAddress = "scopespec1qjqqa2yyc8rprm9yxltnymdksjrszkrh4f"
            val expectedSpecUuid = UUID.fromString("800ea884-c1c6-11ec-a437-d7326db68487")
            val contractSpecAddress = "contractspec1q0fctw7rdygy4v5y08j2mqlxs2qqq6hk8z"
            val expectedContractSpecUuid = UUID.fromString("d385bbc3-6910-4ab2-8479-e4ad83e68280")
            val recordName = "totallyGreatRecord"
            val result = assetUtils.buildNewScopeMetadataTransaction(
                scopeId = testScopeId,
                scopeHash = scopeHash,
                owner = testKeyPair.public.getAddress(mainNet = false),
                scopeSpecAddress = specAddress,
                contractSpecAddress = contractSpecAddress,
                recordSpec = RecordSpecification.newBuilder().also { recordSpec ->
                    recordSpec.name = recordName
                    recordSpec.addInputs(InputSpecification.newBuilder().also { inputSpec ->
                        inputSpec.name = recordName
                        inputSpec.typeName = String::class.qualifiedName
                    })
                }.build(),
            )
            // Sanity check: All three messages should be generated (MsgWriteScopeRequest, MsgWriteSessionRequest, MsgWriteRecordRequest)
            Assertions.assertEquals(3, result.messagesCount)
            val writeScopeRequest = result.getMessages(0).unpack(MsgWriteScopeRequest::class.java)
            val writeSessionRequest = result.getMessages(1).unpack(MsgWriteSessionRequest::class.java)
            val writeRecordRequest = result.getMessages(2).unpack(MsgWriteRecordRequest::class.java)
            // Verify that the provided spec address was properly established as the spec uuid
            Assertions.assertEquals(expectedSpecUuid, UUID.fromString(writeScopeRequest.specUuid))
            // Verify that the provided spec address was properly established as the scope's specificationId
            Assertions.assertArrayEquals(MetadataAddress.forScopeSpecification(expectedSpecUuid).bytes, writeScopeRequest.scope.specificationId.toByteArray())
            // Verify that the provided contract spec address was properly established as the contract spec id in the write session request
            Assertions.assertEquals(contractSpecAddress, writeSessionRequest.session.specificationId.toByteArray().let(MetadataAddress::fromBytes).toString())
            // Verify that the provided contract spec address was properly established as the contract spec uuid in the write record request
            Assertions.assertEquals(expectedContractSpecUuid, UUID.fromString(writeRecordRequest.contractSpecUuid))
            // Verify that the provided contract spec address and record name was properly used to establish the record spec address
            Assertions.assertEquals(
                MetadataAddress.forRecordSpecification(expectedContractSpecUuid, recordName).toString(),
                writeRecordRequest.record.specificationId.toByteArray().let(MetadataAddress::fromBytes).toString()
            )
            // Verify that the provided record name is set properly in the write record request
            Assertions.assertEquals(recordName, writeRecordRequest.record.name)
        }
    }

}
