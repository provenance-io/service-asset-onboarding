package io.provenance.asset.cli

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.io.BaseEncoding
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import cosmos.crypto.secp256k1.Keys
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.asset.sdk.AssetUtils
import io.provenance.asset.sdk.AssetUtilsConfig
import io.provenance.asset.sdk.LoanServicingUtils
import io.provenance.asset.sdk.ObjectStoreConfig
import io.provenance.asset.sdk.SpecificationConfig
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import io.provenance.client.grpc.Signer
import io.provenance.client.wallet.NetworkType
import io.provenance.hdwallet.bip39.MnemonicWords
import io.provenance.hdwallet.wallet.Account
import io.provenance.hdwallet.wallet.Wallet
import io.provenance.attribute.v1.AttributeType
import io.provenance.attribute.v1.MsgAddAttributeRequest
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
import io.provenance.name.v1.MsgBindNameRequest
import io.provenance.name.v1.NameRecord
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import kotlinx.cli.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create
import tech.figure.asset.v1beta1.Asset
import io.provenance.asset.sdk.extensions.toJson

// TODO: move to a common lib
data class TermsOfServiceAcceptance(
    val version: String
)

class Application {

    companion object {
        const val appName = "Asset Onboard CLI"
        const val version = "0.0.1"

        const val TestnetExplorerUri = "https://explorer.test.provenance.io"
        const val MainnetExplorerUri = "https://explorer.provenance.io"

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

        const val DefaultAssetOnboardUri = "http://localhost:8080"
        const val DefaultApiKey = ""

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
        val `onboard-uri` by option(ArgType.String, shortName = "u", description = "Asset Onboard API URI").default(
            DefaultAssetOnboardUri
        )
        val `api-key` by option(ArgType.String, shortName = "a", description = "Asset Onboard API key").default(
            DefaultApiKey
        )

        override fun execute() {
            shouldExecute = true
        }

        @ExperimentalStdlibApi
        fun run(pbClient: PbClient, account: Account, isTestnet: Boolean) {
            val rawLog = `raw-log`
            val onboardUri = `onboard-uri`
            val apiKey = `api-key`

            //val scopeSpec = BaseEncoding.base64().encode(MetadataAddress.forScopeSpecification(UUID.fromString(DefaultScopeSpecId)).bytes)
            //val scopeSpec = MetadataAddress.forScopeSpecification(UUID.fromString(DefaultScopeSpecId))
            //println("scopeSpec=${scopeSpec}")
            //return

            val address = account.address.value
            val signer = object : Signer {
                override fun address(): String = address

                override fun pubKey(): Keys.PubKey =
                    Keys.PubKey
                        .newBuilder()
                        .setKey(ByteString.copyFrom(account.keyPair.publicKey.compressed()))
                        .build()

                override fun sign(data: ByteArray): ByteArray = account.sign(data)
            }

            println("Requestor key address $address")

            val inputFile = File(input)
            if (inputFile.exists()) {
                // load the asset from the input file
                val assetBytes: ByteArray = inputFile.readBytes()

                val isAsset = try {
                    val assetBuilder = Asset.newBuilder()
                    JsonFormat.parser().merge(String(assetBytes, StandardCharsets.UTF_8), assetBuilder)
                    val asset: Asset = assetBuilder.build()
                    true
                } catch (t: Throwable) {
                    false
                }

                val objectMapper = ObjectMapper().configureProvenance()

                val httpLogger: HttpLoggingInterceptor.Logger = object : HttpLoggingInterceptor.Logger {
                    override fun log(message: String) = println(message)
                }

                val httpClient = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor(httpLogger).apply {
                        level = when(rawLog) {
                            true -> HttpLoggingInterceptor.Level.BODY
                            false -> HttpLoggingInterceptor.Level.NONE
                        }
                    })
                    .callTimeout(Duration.ofSeconds(60))
                    .connectTimeout(Duration.ofSeconds(60))
                    .readTimeout(Duration.ofSeconds(60))
                    .writeTimeout(Duration.ofSeconds(60))
                    .build()

                val assetOnboardApi = Retrofit.Builder()
                    .baseUrl(onboardUri + "/")
                    .client(httpClient)
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .build()
                    .create<AssetOnboardApi>()

                val publicKey = BaseEncoding.base64().encode(account.keyPair.publicKey.compressed())
                println("Requestor public key $publicKey")

                val response = when (isAsset) {
                    true -> onboardAsset(assetOnboardApi, apiKey, assetBytes, publicKey, signer.address())
                    false -> onboardNFT(assetOnboardApi, apiKey, inputFile, publicKey, signer.address())
                }
                when (response.code()) {
                    200 -> {
                        //println(response.body()!!.json)
                        val txBody = TxOuterClass.TxBody.newBuilder().also {
                            response.body()!!.base64.forEach { tx ->
                                //println("$tx")
                                it.addMessages(Any.parseFrom(BaseEncoding.base64().decode(tx)))
                            }
                        }.build()

                        if (rawLog) {
                            println(txBody.toJson() + "\n")
                        }

                        pbClient.estimateAndBroadcastTx(
                            txBody = txBody,
                            signers = listOf(BaseReqSigner(signer)),
                            mode = BroadcastMode.BROADCAST_MODE_SYNC,
                            gasAdjustment = 1.5
                        ).also {
                            it.txResponse.apply {
                                println("TX (height: $height, txhash: $txhash, code: $code, gasWanted: $gasWanted, gasUsed: $gasUsed)")

                                val explorerUri = when(isTestnet) {
                                    true -> TestnetExplorerUri
                                    false -> MainnetExplorerUri
                                }
                                println("--------")
                                println("View transaction on Provenance explorer here:")
                                println("${explorerUri}/tx/$txhash")
                            }
                        }

                        System.exit(-1)
                    }
                    else -> {
                        println("Error: REST request failed: ${response.code()}")
                        System.exit(-1)
                    }
                }
            } else {
                println("ERROR: File `${input}` does not exist")
                System.exit(-1)
            }
        }

        fun onboardAsset(assetOnboardApi: AssetOnboardApi, apiKey: String?, assetBytes: ByteArray, publicKey: String, address: String) = runBlocking {
            println("Onboarding asset...")
            assetOnboardApi.onboardAsset(
                apiKey = apiKey,
                xPublicKey = publicKey,
                xAddress = address,
                body = String(assetBytes, StandardCharsets.UTF_8)
            )
        }

        fun onboardNFT(assetOnboardApi: AssetOnboardApi, apiKey: String?, file: File, publicKey: String, address: String) = runBlocking {
            println("Onboarding NFT...")
            assetOnboardApi.onboardNFT(
                apiKey = apiKey,
                xPublicKey = publicKey,
                xAddress = address,
                file = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    RequestBody.create("application/octet-stream".toMediaTypeOrNull()!!, file)
                )
            )
        }

    }

    class WriteSpecs(private val type: String = "asset") : Subcommand("write-specs-$type", "Write the $type specifications to the blockchain") {

        var shouldExecute: Boolean = false

        val `raw-log` by option(ArgType.Boolean, shortName = "l", description = "Output TX raw log").default(false)

        override fun execute() {
            shouldExecute = true
        }

        fun run(assetUtils: AssetUtils, loanServicingUtils: LoanServicingUtils, pbClient: PbClient, account: Account) {
            val rawLog = `raw-log`

            val address = account.address.value
            val signer = object : Signer {
                override fun address(): String = address

                override fun pubKey(): Keys.PubKey =
                    Keys.PubKey
                        .newBuilder()
                        .setKey(ByteString.copyFrom(account.keyPair.publicKey.compressed()))
                        .build()

                override fun sign(data: ByteArray): ByteArray = account.sign(data)
            }

            val txBody = if (type == "asset") {
                assetUtils.buildAssetSpecificationMetadataTransaction(address)
            } else if (type == "loan-state") {
                loanServicingUtils.buildAssetSpecificationMetadataTransaction(address)
            } else {
                throw IllegalArgumentException("write-specs is misconfigured for type $type")
            }

            println("Requestor key address $address")
            if (rawLog) {
                println(txBody.toJson() + "\n")
            }

            pbClient.estimateAndBroadcastTx(
                txBody = txBody,
                signers = listOf(BaseReqSigner(signer)),
                mode = BroadcastMode.BROADCAST_MODE_SYNC,
                gasAdjustment = 1.5
            ).also {
                it.txResponse.apply {
                    println("TX (height: $height, txhash: $txhash, code: $code, gasWanted: $gasWanted, gasUsed: $gasUsed)")
                }
            }
        }

    }

    class BindNames : Subcommand("bind-names", "Bind names on the blockchain") {

        companion object {
            var shouldExecute: Boolean = false
        }

        val `root-name` by option(ArgType.String, shortName = "rn", description = "Root Namespace").default(
            DefaultRootNamespace
        )
        val `tos-base-name` by option(ArgType.String, shortName = "tn", description = "Terms of Service Base Name").default(
            DefaultTOSBaseName
        )
        val `ft-tos-restricted-name` by option(ArgType.String, shortName = "fn", description = "Figure Tech Terms of Service Restricted Name").default(
            DefaultFigureTechTOSRestrictedName
        )

        val `raw-log` by option(ArgType.Boolean, shortName = "l", description = "Output TX raw log").default(false)

        override fun execute() {
            shouldExecute = true
        }

        fun run(pbClient: PbClient, account: Account) {
            val rootName = `root-name`
            val tosName = `tos-base-name`
            val figureTechName = `ft-tos-restricted-name`
            val rawLog = `raw-log`

            val address = account.address.value
            val signer = object : Signer {
                override fun address(): String = address

                override fun pubKey(): Keys.PubKey =
                    Keys.PubKey
                        .newBuilder()
                        .setKey(ByteString.copyFrom(account.keyPair.publicKey.compressed()))
                        .build()

                override fun sign(data: ByteArray): ByteArray = account.sign(data)
            }

            println("Requestor key address $address")
            println("Binding name ${figureTechName}.${tosName}.${rootName}")

            // figure-tech.tos.pb
            buildBindNamesTransaction(address, figureTechName, tosName, rootName).let { txBody ->
                if (rawLog) {
                    println(txBody.toJson() + "\n")
                }

                pbClient.estimateAndBroadcastTx(
                    txBody = txBody,
                    signers = listOf(BaseReqSigner(signer)),
                    mode = BroadcastMode.BROADCAST_MODE_SYNC,
                    gasAdjustment = 1.5
                ).also {
                    it.txResponse.apply {
                        println("TX (height: $height, txhash: $txhash, code: $code, gasWanted: $gasWanted, gasUsed: $gasUsed)")
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
        val `root-name` by option(ArgType.String, shortName = "rn", description = "Root Namespace").default(
            DefaultRootNamespace
        )
        val `tos-base-name` by option(ArgType.String, shortName = "tn", description = "Terms of Service Base Name").default(
            DefaultTOSBaseName
        )
        val `ft-tos-restricted-name` by option(ArgType.String, shortName = "fn", description = "Figure Tech Terms of Service Restricted Name").default(
            DefaultFigureTechTOSRestrictedName
        )

        val `raw-log` by option(ArgType.Boolean, shortName = "l", description = "Output TX raw log").default(false)

        override fun execute() {
            shouldExecute = true
        }

        fun run(pbClient: PbClient, account: Account, objectMapper: ObjectMapper) {
            val tosVersion = `tos-version`
            val rootName = `root-name`
            val tosName = `tos-base-name`
            val figureTechName = `ft-tos-restricted-name`
            val rawLog = `raw-log`

            val address = account.address.value
            val signer = object : Signer {
                override fun address(): String = address

                override fun pubKey(): Keys.PubKey =
                    Keys.PubKey
                        .newBuilder()
                        .setKey(ByteString.copyFrom(account.keyPair.publicKey.compressed()))
                        .build()

                override fun sign(data: ByteArray): ByteArray = account.sign(data)
            }

            println("Requestor key address $address")

            // TODO: at some point, move this functionality to a common lib?
            buildAddAttributeTransaction(address, address, "${figureTechName}.${tosName}.${rootName}", tosVersion, objectMapper).let { txBody ->
                if (rawLog) {
                    println(txBody.toJson() + "\n")
                }

                pbClient.estimateAndBroadcastTx(
                    txBody = txBody,
                    signers = listOf(BaseReqSigner(signer)),
                    mode = BroadcastMode.BROADCAST_MODE_SYNC,
                    gasAdjustment = 1.5
                ).also {
                    it.txResponse.apply {
                        println("TX (height: $height, txhash: $txhash, code: $code, gasWanted: $gasWanted, gasUsed: $gasUsed)")
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
        val parser = ArgParser("$appName [$version]")

        val version by parser.option(ArgType.Boolean, shortName = "v", description = "Version").default(false)
        val testnet by parser.option(ArgType.Boolean, shortName = "t", description = "Testnet").default(true)

        // object store settings
        val `object-store-url` by parser.option(ArgType.String, shortName = "o", description = "Object Store URL").default(
            DefaultObjectStoreURL
        )
        val `object-store-timeout` by parser.option(ArgType.Int, shortName = "m", description = "Object Store Timeout (ms)").default(
            DefaultObjectStoreTimeout
        )

        // asset specification settings
        val `contract-spec-id` by parser.option(ArgType.String, shortName = "cs", description = "Contract Specification Id").default(
            DefaultContractSpecId
        )
        val `scope-spec-id` by parser.option(ArgType.String, shortName = "ss", description = "Scope Specification Id").default(
            DefaultScopeSpecId
        )

        // client key settings
        val `key-mnemonic` by parser.option(ArgType.String, shortName = "k", description = "Key Mnemonic").default(
            DefaultKeyMnemonic
        )
        val `keyring-index` by parser.option(ArgType.Int, shortName = "r", description = "Keyring Index").default(
            DefaultKeyRingIndex
        )
        val `key-index` by parser.option(ArgType.Int, shortName = "y", description = "Key Index").default(
            DefaultKeyIndex
        )

        // provenance node settings
        val `chain-id` by parser.option(ArgType.String, shortName = "c", description = "Provenance Chain ID").default(
            DefaultProvenanceChainId
        )
        val node by parser.option(ArgType.String, shortName = "n", description = "Provenance RPC node endpoint").default(
            DefaultProvenanceNode
        )

        // commands
        val onboard = Onboard()
        val writeSpecsAsset = WriteSpecs("asset")
        val writeSpecsLoanState = WriteSpecs("loan-state")
        val bindNames = BindNames()
        val acceptTos = AcceptTOS()
        parser.subcommands(
            onboard,
            writeSpecsAsset,
            writeSpecsLoanState,
            bindNames,
            acceptTos
        )

        // parse the arguments
        parser.parse(args)

        if (version) {
            println("${Companion.version}")
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
        val assetUtilsConfig = AssetUtilsConfig(
            osConfig = ObjectStoreConfig(
                url = objectStoreUrl,
                timeoutMs = objectStoreTimeout,
            ),
            specConfig = SpecificationConfig(
                contractSpecId = UUID.fromString(contractSpecId),
                scopeSpecId = UUID.fromString(scopeSpecId),
            ),
        )
        val assetUtils: AssetUtils = AssetUtils(assetUtilsConfig)
        val loanServicingUtils: LoanServicingUtils = LoanServicingUtils(assetUtilsConfig)

        // import the HD wallet from the mnemonic
        val wallet = Wallet.fromMnemonic(
            hrp = NetworkType.TESTNET.prefix,
            passphrase = "",
            mnemonicWords = MnemonicWords.of(keyMnemonic),
            testnet = testnet
        )

        // get the account
        val accountPath = when (testnet) {
            true -> "m/44'/1'/0'/$keyRingIndex/$keyIndex'"
            false -> "m/505'/1'/0'/$keyRingIndex/$keyIndex"
        }
        val account: Account = wallet[accountPath]

        // create the provenance client
        val pbClient = PbClient(
            chainId,
            URI(node),
            GasEstimationMethod.MSG_FEE_CALCULATION
        )

        // run the specified command
        if (Onboard.shouldExecute) {
            onboard.run(pbClient, account, testnet)
        }
        else if (writeSpecsAsset.shouldExecute) {
            writeSpecsAsset.run(assetUtils, loanServicingUtils, pbClient, account)
        }
        else if (writeSpecsLoanState.shouldExecute) {
            writeSpecsLoanState.run(assetUtils, loanServicingUtils, pbClient, account)
        }
        else if (BindNames.shouldExecute) {
            bindNames.run(pbClient, account)
        }
        else if (AcceptTOS.shouldExecute) {
            acceptTos.run(pbClient, account, objectMapper)
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
