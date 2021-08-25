package tech.figure.asset.sdk

import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
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
    }

    val testAsset: Asset = Asset.newBuilder().also { asset ->
        asset.id = tech.figure.util.UUID.newBuilder().also { uuid ->
            uuid.value = UUID.randomUUID().toString()
        }.build()
        asset.type = ASSET_TYPE
        asset.name = ASSET_NAME
        // TODO: asset.payload["TestKey1"] =
    }.build()

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

}
