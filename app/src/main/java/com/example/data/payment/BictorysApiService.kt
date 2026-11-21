package com.example.data.payment

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BictorysApiService {

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("pay/v1/charges")
    suspend fun createCharge(
        @Header("X-Api-Key") apiKey: String,
        @Query("payment_type") paymentType: String? = null,
        @Body request: BictorysCreateChargeRequest
    ): Response<BictorysCreateChargeResponse>

    @Headers("Accept: application/json")
    @GET("pay/v1/transactions/{id}/status")
    suspend fun getTransactionStatus(
        @Header("X-Api-Key") apiKey: String,
        @Path("id") transactionId: String
    ): Response<BictorysTransactionStatusResponse>

    @Headers("Accept: application/json")
    @GET("pay/v1/transactions/search")
    suspend fun searchTransactionByReference(
        @Header("X-Api-Key") apiKey: String,
        @Query("payment_reference") paymentReference: String
    ): Response<BictorysTransactionStatusResponse>

    companion object {
        fun createMoshi(): Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        fun create(baseUrl: String = BictorysConfig.resolveBaseUrl()): BictorysApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(createMoshi()))
                .build()
                .create(BictorysApiService::class.java)
        }
    }
}
