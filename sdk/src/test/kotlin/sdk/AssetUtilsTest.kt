package tech.figure.asset.sdk

import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.encryption.util.getAddress
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
                Assertions.assertEquals(result.second.messagesCount, 2)

                // the first message is the write-scope request
                result.second.getMessages(0).unpack(MsgWriteScopeRequest::class.java).also { writeScope ->
                    // there's only one signer
                    Assertions.assertEquals(writeScope.signersCount, 1)
                    writeScope.getSigners(0).also { signer ->
                        Assertions.assertEquals(signer, testKeyPair.public.getAddress(false))
                    }

                    // there's only one owner
                    Assertions.assertEquals(writeScope.scope.ownersCount, 1)
                    writeScope.scope.getOwners(0).also { owner ->
                        Assertions.assertEquals(owner.address, testKeyPair.public.getAddress(false))
                        Assertions.assertEquals(owner.role, PartyType.PARTY_TYPE_OWNER)
                    }

                    // scope id
                    Assertions.assertEquals(UUID.fromString(writeScope.scope.scopeId.toStringUtf8()), testScopeId)

                    // value owner
                    Assertions.assertEquals(writeScope.scope.valueOwnerAddress, testKeyPair.public.getAddress(false))
                }

                // the second message is the write-record request
                result.second.getMessages(1).unpack(MsgWriteRecordRequest::class.java).also { writeRecord ->
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
                        Assertions.assertEquals(input.status, RecordInputStatus.RECORD_INPUT_STATUS_RECORD)
                    }
                }
            }
        }
    }

}
