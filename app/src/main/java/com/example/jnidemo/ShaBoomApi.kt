package com.example.jnidemo

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path


private const val BASE_URL = "http://10.0.2.2:8000/"

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .build()

interface MarsAPIService {
    @GET("song_file/{song_id}")
    suspend fun getSongFile(
        @Path(
            value = "song_id",
            encoded = true
        ) song_id: Int
    ): Response<ResponseBody>

}

object ShaBoomApi {
    val retrofitService: MarsAPIService by lazy {
        retrofit.create(MarsAPIService::class.java)
    }
}
