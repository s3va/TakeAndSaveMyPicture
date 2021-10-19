package tk.kvakva.takeandsavemypicture

import android.os.Build
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

interface WebApiService {
    @GET
    suspend fun getPicture(@Url url: String) : Response<ResponseBody>
/*
    @GET("/bot{token}/getMe")
    suspend fun getMe(
        @Path("token") token: String
    ): Response<ResponseBody>

    @GET("/bot{token}/sendMessage")
    suspend fun sendMessageToTlg(
        @Path("token") token: String,
        @Query("chat_id") chat_Id: String,
        @Query("text") textMess: String
    ): Response<ResponseBody>

    @Multipart
    @POST("/bot{token}/sendPhoto")
    suspend fun sendPhoto(
        @Path("token") token: String,
        @Query("chat_id") chat_Id: String,
        @Part part1: MultipartBody.Part,
        @Part part2: MultipartBody.Part
    ): Response<ResponseBody>

    @Multipart
    @POST("/bot{token}/sendPhoto")
    suspend fun sendPhoto(
        @Path("token") token: String,
        @Query("chat_id") chat_Id: String,
        @Part part: MultipartBody.Part,
    ): Response<ResponseBody>
*/

    /*@GET("info/serviceList")
    suspend fun getBeeOptions(@Query("ctn") number: String, @Query("token") token: String):
            BeeOptionsData*/
}

val singlRetrofit: Retrofit by lazy {
    Retrofit.Builder()
        //.addConverterFactory(MoshiConverterFactory.create(moshi))
        //  .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)

                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    }
                )

                .build()
        )
        .baseUrl("https://127.0.0.1")
        .build()
}

val retrofitService: WebApiService by lazy {
    singlRetrofit.create(WebApiService::class.java)
}

fun datelocaltimestring() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    LocalDateTime.now().toString()
} else {
    Date().toString()
}
