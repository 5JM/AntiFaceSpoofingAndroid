package aero.cubox.communication.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception

//singleton
object RetrofitClient {
    private var retrofitClient : Retrofit? = null

    fun getClient(baseUrl:String):Retrofit?{
//        Log.d("Retrofit>>","RetrofitClient-getClient() call")

        if(retrofitClient ==null){
            retrofitClient = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitClient
    }
}