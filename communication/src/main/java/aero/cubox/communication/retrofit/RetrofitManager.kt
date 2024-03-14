package aero.cubox.communication.retrofit

import aero.cubox.communication.SharedData
import aero.cubox.communication.utils.EnrollBody
import aero.cubox.communication.utils.HistoryBody
import android.util.Log
import com.google.gson.JsonElement
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private class AuthInterceptor : Interceptor {
//    private var count = 0

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        Log.e("Interceptor>>",""+request.url())
        val response = chain.proceed(request)
//        when(response.code()){
//            401, 403, 404-> {
//                //Show UnauthorizedError Message & Show NotFound Message
//                if(count<1) {
//                    SharedData.enrollDevice(SharedData.activity)
//                    count+=1
//                }
//            }
//        }
        return response
    }
}
class RetrofitManager {
    private val TAG = "CUBOX"
    companion object{
        val instance = RetrofitManager()
    }
    private var api = SharedData.apis

    val builder = OkHttpClient().newBuilder().addInterceptor(AuthInterceptor())
    val client = builder.build()

    lateinit var retrofitInterface : RetrofitInterface

    fun initRetrofit():RetrofitInterface{
        val _retrofit = Retrofit.Builder()
            .baseUrl(api.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(RetrofitInterface::class.java)
        return _retrofit
    }

    fun tamperFail(
        token : String, live_image : MultipartBody.Part, id_image : MultipartBody.Part,
        score1 : MultipartBody.Part, score2 : MultipartBody.Part, score3 : MultipartBody.Part, score4 : MultipartBody.Part,
        thr1 : MultipartBody.Part, thr2 : MultipartBody.Part, thr3 : MultipartBody.Part, thr4 : MultipartBody.Part,
        errCode : MultipartBody.Part, errValue : MultipartBody.Part

    ){
        val call = retrofitInterface?.tamperFail(
            "Bearer $token", id_image, live_image,
            score1, score2, score3, score4, thr1, thr2, thr3, thr4,
            errCode, errValue
        )

        call.enqueue(object : Callback<JsonElement>{
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
//                    val str = response.body().toString()
                    Log.e("Test>>", "Suc tamp - fail")
                }else{
                    Log.e(TAG,"tamperFail ${response.code()}")
                    Log.e(TAG,"tamperFail ${response.errorBody()?.string()}")
                }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.e(TAG,"tamperFail onFailure")
            }
        })
    }

    //1:1
    fun faceVerification(
        token : String, live_image : MultipartBody.Part, id_image : MultipartBody.Part, completion:(HashMap<String, String>)->Unit){
        //header 할당
        val call = retrofitInterface?.faceVerification(
            "Bearer $token", id_image, live_image
        ).let {
            it
        }?:return
        val map = HashMap<String,String>()
        call.enqueue(object : Callback<JsonElement>{
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    Log.d(TAG, "Suc verification")
                    val str = response.body().toString()
                    val jsObj = JSONObject(str)
                    val jsArr = jsObj.getJSONObject("data")
                    val match = jsArr.getBoolean("match")
                    val score = jsArr.getDouble("score")
                    val thr = jsArr.getDouble("thresholder")
                    map.put("match", match.toString())
                    map.put("score", score.toString())
                    map.put("thr", thr.toString())
                    map.put("msg", "${response.code()}")

                    completion(map)

                }else{
//                    Log.e("Test>>","response : ${response.errorBody()?.string()}")

                    if(response.body()!=null) {
                        val str = response.body().toString()

                        val jsObj = JSONObject(str)
                        val errMsg = jsObj.getString("errorResponse")
                        map.put("errMsg", errMsg)
                    }
                    map.put("msg", "${response.code()}")

                    completion(map)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.e("Verification>>","onFailure")
                map.put("msg", "Check the Internet!")
                completion(map)
            }
        })
    }

    fun enrollDevice(dUuid:String, type : String, os : String, manufac : String, ip:String, mac:String, completion:(HashMap<String,String>)->Unit){
        val requestMap = EnrollBody(dUuid,type, os,manufac,ip,mac)
        val call = retrofitInterface?.enrollDevice(requestMap).let {
            it
        }?:return
        val map = HashMap<String,String>()
        call.enqueue(object : Callback<JsonElement>{
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful){
                    val jsArr = JSONObject(response.body().toString()).getJSONObject("data")
                    val token = jsArr.getString("token")
                    val deviceId = jsArr.getString("deviceId")

                    map.put("msg",response.code().toString())
                    if (token != null) {
                        map["token"] = token
                    }
                    if (deviceId != null) {
                        map["device_id"] = deviceId
                    }
                    completion(map)
                }else{
                    if(response.body()!=null) {
                        val errMsg = JSONObject(response.body().toString()).getString("errorResponse")
                        map.put("errMsg", errMsg)
                    }
                    Log.e(TAG,"Enroll errCode "+response.code().toString())
                    map.put("msg",response.code().toString())
                    completion(map)
                }

            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.e(TAG,"enroll_fail")
                map.put("msg","Check the Internet!")
                completion(map)
            }
        })
    }

    fun upHistory(token : String, id: String,code : String,value : String){
        val historyBody = HistoryBody(code,value,"")

        val call = retrofitInterface?.saveHistory("Bearer $token", id, historyBody).let {
            it
        }?:return
        call.enqueue(object : Callback<JsonElement>{
            //            val map = HashMap<String,String>()
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful){
//                    val str = response.body().toString()
//                    Log.e(TAG,"histroy ${response.code()}")
                }else{
                    val str = response.raw()
                    Log.e(TAG,"Err\n${str}")
                    if(response.body()!=null){
//                        Log.e("History>>","Body\n${response.body()}")
                        Log.e(TAG,"histroy ${response.code()}")

                        when(response.code().toString()){
                            "401","403","404"->{
                                SharedData.tokenFlag = false
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.e(TAG,"Check the Internet!")
            }
        })
    }
    fun getThreshHold(completion:(HashMap<String,String>)->Unit){
        val call = retrofitInterface?.getDeviceThreshold().let{
            it
        }?:return
        call.enqueue(object : Callback<JsonElement>{
            val map = HashMap<String,String>()
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful){
                    val str = response.body().toString()
                    val jsObj = JSONObject(str)
                    val jsArr = jsObj.getJSONArray("data")
//                    val tt = jsArr.getJSONArray("data")
                    val t1 = jsArr.getJSONObject(0)
                    val t2 = jsArr.getJSONObject(1)
                    val t3 = jsArr.getJSONObject(2)
                    val t4 = jsArr.getJSONObject(3)

                    map[t1.getString("code")] = t1.get("value").toString()
                    map[t2.getString("code")] = t2.get("value").toString()
                    map[t3.getString("code")] = t3.get("value").toString()
                    map[t4.getString("code")] = t4.get("value").toString()

                    completion(map)
                }else{
                    Log.e(TAG,"Thresh hold errCode ${response.code()}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.e(TAG,"Thresh hold Fail")
            }
        })
    }
}