package io.provenance.asset.cli

import com.fasterxml.jackson.databind.node.ObjectNode
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.Response

data class TxBody(
    val json: ObjectNode,
    val base64: List<String>
)

interface AssetOnboardApi  {

    @Headers("Content-Type: application/json")
    @POST("api/v1/asset")
    suspend fun onboardAsset(
        @Header("apikey") apiKey: String? = null,
        @Header("x-public-key") xPublicKey: String,
        @Header("x-address") xAddress: String,
        @Body body: String
    ): Response<TxBody>

    @Multipart
    @POST("api/v1/asset/file")
    suspend fun onboardNFT(
        @Header("apikey") apiKey: String? = null,
        @Header("x-public-key") xPublicKey: String,
        @Header("x-address") xAddress: String,
        @Part file: MultipartBody.Part
    ): Response<TxBody>

}
