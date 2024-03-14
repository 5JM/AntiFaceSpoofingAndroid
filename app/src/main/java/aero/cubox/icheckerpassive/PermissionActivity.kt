package aero.cubox.icheckerpassive

import aero.cubox.communication.SharedData
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaDrm
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.*

class PermissionActivity : AppCompatActivity() {
    val MULTIPLE_PERMISSIONS = 77
    var permissionListener = MutableLiveData<Boolean>()

    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        val button=findViewById<TextView>(R.id.permissionGuideButton)

        val intent = Intent(this,MainActivity::class.java)

        // 권한이 이미 있으면 바로 mainActivity로 이동
        var result = false

        permissions.forEach { pm ->
            result = (ContextCompat.checkSelfPermission(this, pm) == PackageManager.PERMISSION_GRANTED)
        }

        if (result) startIntent(intent)

        button.setOnClickListener {
            permissionListener.value=checkPermissions()
            permissionListener.observe(this, Observer {
                if(it==true) startIntent(intent)
            })
        }
    }

    private fun checkPermissions(): Boolean {
        var result: Int
        val permissionList: MutableList<String> = ArrayList()
        for (pm in permissions) {
            result = ContextCompat.checkSelfPermission(this, pm)
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm)
            }
        }
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionList.toTypedArray(),
                MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }

    private fun getManufacturer():String{
        val rawM = Build.MANUFACTURER
        var result = "n"
        when(rawM.lowercase()){
            "samsung"->{
                result = "ss"
            }
            "lge"->{
                result = "lg"
            }
        }
        return result
    }

    private fun startIntent(intent: Intent){
        val uuid = GetDevicesUUID(this)
//      https://shinhaninvest-api.cubox-pd.com/v1/ -> BASE_URL_V1
//      https://shinhaninvest-api.cubox-pd.com/v2/ -> BASE_URL

        SharedData.setInParameters(
            null,
            getManufacturer(),  // 제조사 * Optional
            "a",  // os ( android )-> a를 넣어주면됨 * Optional
            uuid,
//            BuildConfig.BASE_URL_V1
            BuildConfig.BASE_URL
        )
        // ios -> 32자
//      "ffffffff-b5f1-5e94-0033-c5870033c587" <- old
//      "RjueuQV6RL3a9stzS6wk1ulx+gqEEIDNe2SiAjRpzdM=" <- new android 10 (44)
        startActivity(intent)
        this.finish()
    }

    /**
     * checkPermission 메소드를 실행한 후 콜백 메소드
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MULTIPLE_PERMISSIONS -> {
                var isDeny = false
                if (grantResults.isNotEmpty()) {
                    var i = 0
                    while (i < permissions.size) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            //permission denied
                            isDeny = true
                        }
                        i++
                    }
                }
                if (isDeny) showNoPermissionToastAndFinish()
                else permissionListener.postValue(true)
            }
        }
    }

    /**
     * 권한 거부 시 실행 메소드
     */
    private fun showNoPermissionToastAndFinish() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("서비스 이용 알림")
        builder.setMessage("필수 권한을 허용해야\n서비스 정상 이용이 가능합니다.\n권한 요청 시 반드시 허용해주세요.")
        builder.setPositiveButton(
            "확인"
        ) { dialog, which ->
            moveTaskToBack(true) // 태스크를 백그라운드로 이동
            finishAndRemoveTask() // 액티비티 종료 + 태스크 리스트에서 지우기
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    // Android 10 이상 UUID 추출
    private fun GetDevicesUUID(context: Context):String?{
        var deviceUuid : UUID?  = null
        var deviceId : String? = null
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ""
        }
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            deviceUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
            try{
                val mediaDrm = MediaDrm(deviceUuid)
                deviceId = android.util.Base64.encodeToString(mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID), 0).trim()
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }else{
            deviceId = GetDevicesUUIDOld(context)
        }
        val sb = StringBuilder()
        sb.append(deviceId?.slice(IntRange(0,7)))
        sb.append("-")
        sb.append(deviceId?.slice(IntRange(8,11)))
        sb.append("-")
        sb.append(deviceId?.slice(IntRange(12,15)))
        sb.append("-")
        sb.append(deviceId?.slice(IntRange(16,19)))
        sb.append("-")
        sb.append(deviceId?.slice(IntRange(20,deviceId.length-1)))

        deviceId = sb.toString()
//        ffffffff-b5f1-5e94-0033-c5870033c587 <- 예전 uuid형식
//        RjueuQV6-RL3a-9stz-S6wk-1ulx-+gqEEIDNe2SiAjRpzdM= <- 안드로이드10 이상 uuid형식
        return deviceId
    }


    // 안드로이드 9 이하 UUID 추출
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun GetDevicesUUIDOld(mContext: Context): String? {
        val tm = mContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ""
        }
        val tmDevice: String = "" + tm.deviceId
        val tmSerial: String = "" + tm.simSerialNumber
        val androidId: String = "" + Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val deviceUuid = UUID(
            androidId.hashCode().toLong(), tmDevice.hashCode()
                .toLong() shl 32 or tmSerial.hashCode().toLong()
        )
        val deviceId = deviceUuid.toString()

        return deviceId
    }
}