package com.example.helmetdetector

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

/**
 * Interface defining the API endpoints for helmet detection
 */
interface HelmetDetectionApiService {

    @Multipart
    @POST("predict/")
    suspend fun predictHelmet(
        @Part image: MultipartBody.Part
    ): Response<ResponseBody>

    companion object {
        // Update this URL with your actual Hugging Face API URL
        private const val BASE_URL = "https://bhavyapatel9-helmet-detector.hf.space/"

        fun create(): HelmetDetectionApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HelmetDetectionApiService::class.java)
        }
    }
}