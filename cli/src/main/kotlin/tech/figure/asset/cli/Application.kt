package tech.figure.asset.cli

import com.figure.wallet.account.InMemoryKeyHolder
import com.figure.wallet.account.Key
import com.figure.wallet.pbclient.client.GrpcClient
import com.figure.wallet.pbclient.client.GrpcClientOpts
import com.google.common.io.BaseEncoding
import com.google.protobuf.util.JsonFormat
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import io.provenance.scope.encryption.ecies.ECUtils
import kotlinx.cli.*
import tech.figure.asset.Asset
import tech.figure.asset.sdk.AssetUtils
import tech.figure.asset.sdk.AssetUtilsConfig
import tech.figure.asset.sdk.ObjectStoreConfig
import tech.figure.asset.sdk.extensions.toBase64String
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets

class Application {

    companion object {
        const val appName = "Asset Onboard CLI"
        const val version = "0.0.1"

        const val DefaultObjectStoreURL = "grpc://localhost:8081"
        const val DefaultObjectStoreTimeout = 30000

        // tp1mryqzguyelef5dae7k6l22tnls93cvrc60tjdc
        const val DefaultKeyMnemonic = "jealous bright oyster fluid guide talent crystal minor modify broken stove spoon pen thank action smart enemy chunk ladder soon focus recall elite pulp"
        const val DefaultKeyRingIndex = 0
        const val DefaultKeyIndex = 0

        const val DefaultProvenanceChainId = "chain-local"
        const val DefaultProvenanceNode = "https://localhost:9090"
    }

    class Onboard : Subcommand("onboard", "Onboard an asset") {

        companion object {
            var shouldExecute: Boolean = false
        }

        val input by argument(ArgType.String, description = "Input asset file")
        val `raw-log` by option(ArgType.Boolean, shortName = "l", description = "Output TX raw log").default(false)

        override fun execute() {
            shouldExecute = true
        }

        fun run(assetUtils: AssetUtils, pbClient: GrpcClient, key: Key) {
            val rawLog = `raw-log`
            val publicKey = ECUtils.convertBytesToPublicKey(key.publicKey().toByteArray())
            val address = key.address().getValue()

            println("Requestor key address $address")

            val inputFile = File(input)
            if (inputFile.exists()) {
                // load the asset from the input file
                val assetBytes: ByteArray = inputFile.readBytes()

                // encrypt and store the asset in the object-store
                var hash = ""
                try {
                    val assetBuilder = Asset.newBuilder()
                    JsonFormat.parser().merge(String(assetBytes, StandardCharsets.UTF_8), assetBuilder)
                    val asset: Asset = assetBuilder.build()

                    try {
                        hash = assetUtils.encryptAndStore(asset, publicKey).toBase64String()
                        println("Encrypted and stored asset ${asset.id} in object store with hash $hash for publicKey ${BaseEncoding.base64().encode(key.publicKey().toByteArray())}")
                    } catch (t: Throwable) {
                        println("ERROR: Failed to encrypt and store the asset. Reason=${t.message?:t.cause?.message}")
                        System.exit(-1)
                    }
                } catch (t: Throwable) {
                    println("ERROR: File `${input}` does not contain an asset protobuf and blobs are not allowed.")
                    System.exit(-1)
                }

                // generate the Provenance metadata TX message for this asset scope
                assetUtils.buildNewScopeMetadataTransaction(address, "Record", mapOf("Asset" to hash)).let {
                    val scopeId = it.first
                    val txBody = it.second

                    println("Created new scope $scopeId")

                    val baseReq = pbClient.baseRequest(
                        key = key,
                        txBody = txBody
                    )

                    // simulate the TX
                    val gasEstimate = pbClient.estimateTx(baseReq)

                    // broadcast the TX
                    println("Broadcasting metadata TX (estimated gas: ${gasEstimate.estimate}, estimated fees: ${gasEstimate.fees} nhash)...")
                    pbClient.broadcastTx(baseReq, gasEstimate, BroadcastMode.BROADCAST_MODE_BLOCK).also {
                        it.txResponse.apply {
                            println("TX (height: $height, txhash: $txhash, code: $code, gasWanted: $gasWanted, gasUsed: $gasUsed)")
                            if(rawLog) {
                                println("LOG $rawLog")
                            }
                        }
                    }
                }
            } else {
                println("ERROR: File `${input}` does not exist")
                System.exit(-1)
            }
        }

    }

    fun main(args: Array<String>) {
        val parser = ArgParser("${Application.appName} [${Application.version}]")

        val version by parser.option(ArgType.Boolean, shortName = "v", description = "Version").default(false)
        val testnet by parser.option(ArgType.Boolean, shortName = "t", description = "Testnet").default(true)

        // object store settings
        val `object-store-url` by parser.option(ArgType.String, shortName = "o", description = "Object Store URL").default(Application.DefaultObjectStoreURL)
        val `object-store-timeout` by parser.option(ArgType.Int, shortName = "m", description = "Object Store Timeout (ms)").default(Application.DefaultObjectStoreTimeout)

        // client key settings
        val `key-mnemonic` by parser.option(ArgType.String, shortName = "k", description = "Key Mnemonic").default(Application.DefaultKeyMnemonic)
        val `keyring-index` by parser.option(ArgType.Int, shortName = "r", description = "Keyring Index").default(Application.DefaultKeyRingIndex)
        val `key-index` by parser.option(ArgType.Int, shortName = "y", description = "Key Index").default(Application.DefaultKeyIndex)

        // provenance node settings
        val `chain-id` by parser.option(ArgType.String, shortName = "c", description = "Provenance Chain ID").default(Application.DefaultProvenanceChainId)
        val node by parser.option(ArgType.String, shortName = "n", description = "Provenance RPC node endpoint").default(Application.DefaultProvenanceNode)

        // commands
        val onboard = Onboard()
        parser.subcommands(
            onboard
        )

        // parse the arguments
        parser.parse(args)

        if (version) {
            println("${Application.version}")
        }

        val objectStoreUrl = `object-store-url`
        val objectStoreTimeout = `object-store-timeout`.toLong()

        val keyMnemonic = `key-mnemonic`
        val keyRingIndex = `keyring-index`
        val keyIndex = `key-index`

        val chainId = `chain-id`

        // create the asset utils
        val assetUtils: AssetUtils = AssetUtils(
            AssetUtilsConfig(
                osConfig = ObjectStoreConfig(
                    url = objectStoreUrl,
                    timeoutMs = objectStoreTimeout
                )
            )
        )

        // import the HD wallet from the mnemonic
        val key = InMemoryKeyHolder
            .fromMnemonic(keyMnemonic, !testnet)
            .keyring(keyRingIndex)
            .key(keyIndex)

        // create the provenance client
        val pbClient = GrpcClient(
            GrpcClientOpts(
                chainId = chainId,
                channelUri = URI(node)
            )
        )

        // run the specified command
        if (Onboard.shouldExecute) {
            onboard.run(assetUtils, pbClient, key)
        }
    }

}

fun main(args: Array<String>) {
    Application().main(args)
}
