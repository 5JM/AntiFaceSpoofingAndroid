package aero.cubox.communication.retrofit

import com.google.gson.JsonElement
import aero.cubox.communication.utils.EnrollBody
import aero.cubox.communication.utils.HistoryBody
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Multipart
import retrofit2.http.POST

interface RetrofitInterface {
    // 위변조 체크 실패 이력 저장
    @Multipart
    @POST("users/tamper-fail")
    fun tamperFail(
        @Header("Authorization") authorization : String,
        @Part Image1 : MultipartBody.Part,
        @Part Image2 : MultipartBody.Part,
        @Part Score1 : MultipartBody.Part,
        @Part Score2 : MultipartBody.Part,
        @Part Score3 : MultipartBody.Part,
        @Part Score4 : MultipartBody.Part,
        @Part Threshold1 : MultipartBody.Part,
        @Part Threshold2 : MultipartBody.Part,
        @Part Threshold3 : MultipartBody.Part,
        @Part Threshold4 : MultipartBody.Part,
        @Part ErrorCode : MultipartBody.Part,
        @Part ErrorValue : MultipartBody.Part
    ) : Call<JsonElement>

    //얼굴인증 1:1
    @Multipart
    @POST("users/verification-aos")
    fun faceVerification(
        @Header("Authorization") authorization : String,
        @Part Image1 : MultipartBody.Part,
        @Part Image2 : MultipartBody.Part
    ): Call<JsonElement>

    //단말등록
    @Headers("Content-Type:application/json")
    @POST("devices")
    fun enrollDevice(
        @Body Body: EnrollBody
    ): Call<JsonElement>

    //단말이력저장
    @Headers("Content-Type:application/json")
    @POST("devices/{id}/history")
    fun saveHistory(
        @Header("Authorization") authorization : String,
        @Path("id",encoded = true) id: String,
        @Body body : HistoryBody
    ): Call<JsonElement>

    //임계치조회
    @Headers("Content-Type:application/json")
    @GET("devices/threshold")
    fun getDeviceThreshold(): Call<JsonElement>
}