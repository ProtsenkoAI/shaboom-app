package com.example.jnidemo

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path


private const val BASE_URL = "http://10.0.2.2:8000/"


private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

data class SongEntry(@field:Json(name = "id") val id: Int,
                     @field:Json(name = "name") val name: String,
                     @field:Json(name = "performer") val performer: String
)

interface ShaboomApiService {
    @GET("song_file/{song_id}")
    suspend fun getSongFile(
        @Path(
            value = "song_id",
            encoded = true
        ) song_id: Int
    ): Response<ResponseBody>

    @GET("song_pitches/{song_id}")
    suspend fun getSongPitches(
        @Path(
            value = "song_id",
            encoded = true
        ) song_id: Int
    ): Response<List<Float>>

    @GET("user_songs")
    suspend fun getUserSongs(): List<SongEntry>

}

object ShaBoomApi {
    val retrofitService: ShaboomApiService by lazy {
        retrofit.create(ShaboomApiService::class.java)
    }
}
