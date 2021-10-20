package tech.figure.asset.cli

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.figure.wallet.account.InMemoryKeyHolder
import com.figure.wallet.account.Key
import com.figure.wallet.pbclient.client.GrpcClient
import com.figure.wallet.pbclient.client.GrpcClientOpts
import com.figure.wallet.pbclient.extension.toAny
import com.figure.wallet.pbclient.extension.toTxBody
import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.attribute.v1.AttributeType
import io.provenance.attribute.v1.MsgAddAttributeRequest
import io.provenance.name.v1.MsgBindNameRequest
import io.provenance.name.v1.NameRecord
import io.provenance.scope.encryption.ecies.ECUtils
import kotlinx.cli.*
import tech.figure.asset.Asset
import tech.figure.asset.sdk.AssetUtils
import tech.figure.asset.sdk.AssetUtilsConfig
import tech.figure.asset.sdk.ObjectStoreConfig
import tech.figure.asset.sdk.SpecificationConfig
import tech.figure.asset.sdk.extensions.toBase64String
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*

// TODO: move to a common lib
data class TermsOfServiceAcceptance(
    val version: String
)

class Application {

    companion object {
        const val appName = "Asset Onboard CLI"
        const val version = "0.0.1"

        const val DefaultObjectStoreURL = "grpc://localhost:8081"
        const val DefaultObjectStoreTimeout = 30000

        const val DefaultContractSpecId = "18573cf8-ddb9-491e-a4cb-bf2176160a63"
        const val DefaultScopeSpecId = "997e8228-c37f-4668-9a66-6cfb3b2a23cd"

        // tp1mryqzguyelef5dae7k6l22tnls93cvrc60tjdc
        const val DefaultKeyMnemonic = "jealous bright oyster fluid guide talent crystal minor modify broken stove spoon pen thank action smart enemy chunk ladder soon focus recall elite pulp"
        const val DefaultKeyRingIndex = 0
        const val DefaultKeyIndex = 0

        const val DefaultProvenanceChainId = "chain-local"
        const val DefaultProvenanceNode = "https://localhost:9090"

        const val DefaultRootNamespace = "pb"
        const val DefaultTOSBaseName = "tos"
        const val DefaultFigureTechTOSRestrictedName = "figure-tech"
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

        @ExperimentalStdlibApi
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
                var scopeId = ""
                try {
                    val assetBuilder = Asset.newBuilder()
                    JsonFormat.parser().merge(String(assetBytes, StandardCharsets.UTF_8), assetBuilder)
                    val asset: Asset = assetBuilder.build()

                    try {
                        hash = assetUtils.encryptAndStore(asset, publicKey).toBase64String()
                        scopeId = asset.id
                        println("Encrypted and stored asset $scopeId in object store with hash $hash for publicKey ${BaseEncoding.base64().encode(key.publicKey().toByteArray())}")
                    } catch (t: Throwable) {
                        println("ERROR: Failed to encrypt and store the asset. Reason=${t.message?:t.cause?.message}")
                        System.exit(-1)
                    }
                } catch (t: Throwable) {
                    println("ERROR: File `${input}` does not contain an asset protobuf and blobs are not allowed.")
                    System.exit(-1)
                }

                // generate the Provenance metadata TX message for this asset scope
                assetUtils.buildNewScopeMetadataTransaction(UUID.fromString(scopeId), hash, address).let { txBody ->
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

    class WriteSpecs : Subcommand("write-specs", "Write the specifications to the blockchain") {

        companion object {
            var shouldExecute: Boolean = false
        }

        val `raw-log` by option(ArgType.Boolean, shortName = "l", description = "Output TX raw log").default(false)

        override fun execute() {
            shouldExecute = true
        }

        fun run(assetUtils: AssetUtils, pbClient: GrpcClient, key: Key) {
            val rawLog = `raw-log`
            val address = key.address().getValue()

            println("Requestor key address $address")

            assetUtils.buildAssetSpecificationMetadataTransaction(address).let { txBody ->
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

        }

    }

    class BindNames : Subcommand("bind-names", "Bind names on the blockchain") {

        companion object {
            var shouldExecute: Boolean = false
        }

        val `root-name` by option(ArgType.String, shortName = "rn", description = "Root Namespace").default(Application.DefaultRootNamespace)
        val `tos-base-name` by option(ArgType.String, shortName = "tn", description = "Terms of Service Base Name").default(Application.DefaultTOSBaseName)
        val `ft-tos-restricted-name` by option(ArgType.String, shortName = "fn", description = "Figure Tech Terms of Service Restricted Name").default(Application.DefaultFigureTechTOSRestrictedName)

        val `raw-log` by option(ArgType.Boolean, shortName = "l", description = "Output TX raw log").default(false)

        override fun execute() {
            shouldExecute = true
        }

        fun run(pbClient: GrpcClient, key: Key) {
            val rootName = `root-name`
            val tosName = `tos-base-name`
            val figureTechName = `ft-tos-restricted-name`
            val rawLog = `raw-log`
            val address = key.address().getValue()

            println("Requestor key address $address")

            println("Binding name ${figureTechName}.${tosName}.${rootName}")

            // figure-tech.tos.pb
            buildBindNamesTransaction(address, figureTechName, tosName, rootName).let { txBody ->
                val baseReq = pbClient.baseRequest(
                    key = key,
                    txBody = txBody
                )

                // simulate the TX
                val gasEstimate = pbClient.estimateTx(baseReq)

                // broadcast the TX
                println("Broadcasting bind name TX (estimated gas: ${gasEstimate.estimate}, estimated fees: ${gasEstimate.fees} nhash)...")
                pbClient.broadcastTx(baseReq, gasEstimate, BroadcastMode.BROADCAST_MODE_BLOCK).also {
                    it.txResponse.apply {
                        println("TX (height: $height, txhash: $txhash, code: $code, gasWanted: $gasWanted, gasUsed: $gasUsed)")
                        if(rawLog) {
                            println("LOG $rawLog")
                        }
                    }
                }
            }

        }

        private fun buildBindNamesTransaction(owner: String, ftTosName: String, tosName: String, root: String): TxOuterClass.TxBody {
            return listOf(

                // bind-name `$tosName.$root`
                MsgBindNameRequest.newBuilder().apply {
                    parent = NameRecord.newBuilder().apply {
                        name = root
                        address = owner
                    }.build()
                    record = NameRecord.newBuilder().apply {
                        name = tosName
                        address = owner
                    }.build()
                }.build().toAny(),

                // bind-name `$ftTosName.$tosName.$root`
                MsgBindNameRequest.newBuilder().apply {
                    parent = NameRecord.newBuilder().apply {
                        name = "${tosName}.${root}"
                        address = owner
                    }.build()
                    record = NameRecord.newBuilder().apply {
                        name = ftTosName
                        address = owner
                    }.build()
                }.build().toAny(),

            ).toTxBody()
        }

    }

    class AcceptTOS : Subcommand("accept-tos", "Accept a service provider's TOS for an account") {

        companion object {
            var shouldExecute: Boolean = false
        }

        val account by option(ArgType.String, shortName = "a", description = "Account Address").required()
        val `tos-version` by option(ArgType.String, shortName = "tv", description = "Terms of Service Version").required()
        val `root-name` by option(ArgType.String, shortName = "rn", description = "Root Namespace").default(Application.DefaultRootNamespace)
        val `tos-base-name` by option(ArgType.String, shortName = "tn", description = "Terms of Service Base Name").default(Application.DefaultTOSBaseName)
        val `ft-tos-restricted-name` by option(ArgType.String, shortName = "fn", description = "Figure Tech Terms of Service Restricted Name").default(Application.DefaultFigureTechTOSRestrictedName)

        val `raw-log` by option(ArgType.Boolean, shortName = "l", description = "Output TX raw log").default(false)

        override fun execute() {
            shouldExecute = true
        }

        fun run(pbClient: GrpcClient, key: Key, objectMapper: ObjectMapper) {
            val tosVersion = `tos-version`
            val rootName = `root-name`
            val tosName = `tos-base-name`
            val figureTechName = `ft-tos-restricted-name`
            val rawLog = `raw-log`
            val address = key.address().getValue()

            println("Requestor key address $address")

            // TODO: at some point, move this functionality to a common lib?

            buildAddAttributeTransaction(address, account, "${figureTechName}.${tosName}.${rootName}", tosVersion, objectMapper).let { txBody ->
                val baseReq = pbClient.baseRequest(
                    key = key,
                    txBody = txBody
                )

                // simulate the TX
                val gasEstimate = pbClient.estimateTx(baseReq)

                // broadcast the TX
                println("Broadcasting add attribute TX (estimated gas: ${gasEstimate.estimate}, estimated fees: ${gasEstimate.fees} nhash)...")
                pbClient.broadcastTx(baseReq, gasEstimate, BroadcastMode.BROADCAST_MODE_BLOCK).also {
                    it.txResponse.apply {
                        println("TX (height: $height, txhash: $txhash, code: $code, gasWanted: $gasWanted, gasUsed: $gasUsed)")
                        if(rawLog) {
                            println("LOG $rawLog")
                        }
                    }
                }
            }

        }

        private fun buildAddAttributeTransaction(owner: String, address: String, name: String, version: String, objectMapper: ObjectMapper): TxOuterClass.TxBody {
            return listOf(

                // add-attribute `$ftTosName.$tosName.$root`
                MsgAddAttributeRequest.newBuilder().apply {
                    this.owner = owner
                    account = address
                    attributeType = AttributeType.ATTRIBUTE_TYPE_JSON
                    this.name = name
                    value = ByteString.copyFrom(objectMapper.writeValueAsBytes(TermsOfServiceAcceptance(version)))
                }.build().toAny(),

            ).toTxBody()
        }

    }

    @ExperimentalStdlibApi
    fun main(args: Array<String>) {
        val parser = ArgParser("${Application.appName} [${Application.version}]")

        val version by parser.option(ArgType.Boolean, shortName = "v", description = "Version").default(false)
        val testnet by parser.option(ArgType.Boolean, shortName = "t", description = "Testnet").default(true)

        // object store settings
        val `object-store-url` by parser.option(ArgType.String, shortName = "o", description = "Object Store URL").default(Application.DefaultObjectStoreURL)
        val `object-store-timeout` by parser.option(ArgType.Int, shortName = "m", description = "Object Store Timeout (ms)").default(Application.DefaultObjectStoreTimeout)

        // asset specification settings
        val `contract-spec-id` by parser.option(ArgType.String, shortName = "cs", description = "Contract Specification Id").default(Application.DefaultContractSpecId)
        val `scope-spec-id` by parser.option(ArgType.String, shortName = "ss", description = "Scope Specification Id").default(Application.DefaultScopeSpecId)

        // client key settings
        val `key-mnemonic` by parser.option(ArgType.String, shortName = "k", description = "Key Mnemonic").default(Application.DefaultKeyMnemonic)
        val `keyring-index` by parser.option(ArgType.Int, shortName = "r", description = "Keyring Index").default(Application.DefaultKeyRingIndex)
        val `key-index` by parser.option(ArgType.Int, shortName = "y", description = "Key Index").default(Application.DefaultKeyIndex)

        // provenance node settings
        val `chain-id` by parser.option(ArgType.String, shortName = "c", description = "Provenance Chain ID").default(Application.DefaultProvenanceChainId)
        val node by parser.option(ArgType.String, shortName = "n", description = "Provenance RPC node endpoint").default(Application.DefaultProvenanceNode)

        // commands
        val onboard = Onboard()
        val writeSpecs = WriteSpecs()
        val bindNames = BindNames()
        val acceptTos = AcceptTOS()
        parser.subcommands(
            onboard,
            writeSpecs,
            bindNames,
            acceptTos
        )

        // parse the arguments
        parser.parse(args)

        if (version) {
            println("${Application.version}")
        }

        val objectStoreUrl = `object-store-url`
        val objectStoreTimeout = `object-store-timeout`.toLong()

        val contractSpecId = `contract-spec-id`
        val scopeSpecId = `scope-spec-id`

        val keyMnemonic = `key-mnemonic`
        val keyRingIndex = `keyring-index`
        val keyIndex = `key-index`

        val chainId = `chain-id`

        // create the object mapper
        val objectMapper = ObjectMapper().configureProvenance()

        // create the asset utils
        val assetUtils: AssetUtils = AssetUtils(
            AssetUtilsConfig(
                osConfig = ObjectStoreConfig(
                    url = objectStoreUrl,
                    timeoutMs = objectStoreTimeout,
                ),
                specConfig = SpecificationConfig(
                    contractSpecId = UUID.fromString(contractSpecId),
                    scopeSpecId = UUID.fromString(scopeSpecId),
                ),
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
        else if (WriteSpecs.shouldExecute) {
            writeSpecs.run(assetUtils, pbClient, key)
        }
        else if (BindNames.shouldExecute) {
            bindNames.run(pbClient, key)
        }
        else if (AcceptTOS.shouldExecute) {
            acceptTos.run(pbClient, key, objectMapper)
        }
    }

}

@ExperimentalStdlibApi
fun main(args: Array<String>) {
    Application().main(args)
}

fun ObjectMapper.configureProvenance(): ObjectMapper = registerKotlinModule()
    .registerModule(JavaTimeModule())
    .registerModule(ProtobufModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)