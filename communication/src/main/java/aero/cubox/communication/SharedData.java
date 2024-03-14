package aero.cubox.communication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import aero.cubox.communication.retrofit.RetrofitManager;
import aero.cubox.communication.utils.APIs;
import aero.cubox.communication.utils.State;

public class SharedData {
    private static String TAG = "CUBOX";
    // Image
    private static Bitmap bitmap = null;
    private static final Object bitmap_lock = new Object();
    public static boolean use_idBitmap = false;
    public static boolean use_liveBitmap = false;
    // Network
    public static RetrofitManager retrofitManager = null;
    public static String token;
    public static String device_id;
    private static String mac;

    // in parameters
    public static Bitmap in_img ;
    public static Bitmap first_live_img;

    public static String in_device;
    public static String in_os;
    public static String in_uuid;

    @SuppressLint("StaticFieldLeak")
    public static Activity activity;
    public static APIs apis =new APIs();

    public static Boolean tokenFlag = true;

    public static Boolean startEstFlag = false;
    public static Boolean duringEst = false;

    // todo Url동적 변경용 - versionFlag를 public static으로 바꾸기
//     false = v2 , true = v1
    // 테스트용은 false 기본설정
    public static MutableLiveData<Boolean> versionFlagT = new MutableLiveData<>(false);
    public static Vector<Float> thrsArr = new Vector<Float>();
    public static Vector<Float> thrs = new Vector<Float>(4);

    public static Boolean firstFlag = true;
    public static Boolean counter = false;
    // live frame 저장 함수
    public static void setLiveBitmap(Bitmap bitmap) {
        synchronized (bitmap_lock) {
            SharedData.bitmap = bitmap;
            use_liveBitmap = true;
        }
    }
    // 신분증 이미지 저장 함수
    public static void setIdBitmap(Bitmap bitmap) {
        synchronized (bitmap_lock) {
            SharedData.in_img = bitmap;
            use_idBitmap = true;
        }
    }

    public static Bitmap getIdBitmap() {
        return SharedData.in_img;
    }

    public static Bitmap getBitmap() { return SharedData.bitmap; }

    // 통신 모듈 init
    public static void createManager(Activity _activity){
        activity = _activity;
        if (retrofitManager == null){
            retrofitManager = RetrofitManager.Companion.getInstance();
            retrofitManager.retrofitInterface = retrofitManager.initRetrofit();

            mac = getMacAddress();

            if(firstFlag){
                enrollDevice(activity);
                firstFlag = false;
            }
        }
    }

    public static RetrofitManager getManager(){
        return retrofitManager;
    }

    public  static void setUrl(String baseUrl)
    {
        if(!baseUrl.isEmpty()){
            apis.setBaseUrl(baseUrl);
            if(retrofitManager != null)
                retrofitManager.retrofitInterface = retrofitManager.initRetrofit();
        }
    }

    // API 호출을 위한 파라미터 설정
    public static void setInParameters(Bitmap _in_img, String _in_device, String _in_os, String _in_uuid, String baseUrl){
        in_img = _in_img;
//        in_userId = _in_userId;
        in_device = _in_device;
        in_os = _in_os;
        in_uuid = _in_uuid;
//        CUBOX_API_KEY = API_KEY;
        if(!baseUrl.isEmpty()){
            apis.setBaseUrl(baseUrl);
            if(retrofitManager != null)
                retrofitManager.retrofitInterface = retrofitManager.initRetrofit();
        }
    }

    //토큰 존재여부 확인
    private static Boolean HaveToken(Activity activity){
        SharedPreferences sp = activity.getSharedPreferences("token",0);
        token = sp.getString("token","");
        device_id = sp.getString("device_id","");

        if(token.equals("")||token.isEmpty()||!tokenFlag||device_id.equals("")||device_id.isEmpty()){
            return false;
        }else{
            return true;
        }
    }

    //mac주소
    private static String getMacAddress() {
        try {
            ArrayList<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif:all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) return "00:00:00:00:00:00";
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X-", b));
                }
                if (res1.length()>0) res1.deleteCharAt(res1.length() - 1);
//                Log.e("Mac>>", "mmmm1 - $res1");
//                Log.e(TAG, "get mac address - suc");
                return res1.toString();
            }
        }catch (Exception e) {
//            Log.e(TAG, "get mac address - err");
            e.printStackTrace();
        }
        Log.e(TAG, "get mac address - err");
        return "00:00:00:00:00:00";
    }

    //IP주소 얻는 함수
    private static String GetLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                        if (inetAddress instanceof Inet4Address) {
//                            Log.e(TAG, "get ip address - suc");
                            return inetAddress.getHostAddress();
                        }
                }
            }
        } catch (SocketException ex) {
//            Log.e(TAG, "get ip address - err");
            ex.printStackTrace();
        }
        Log.e(TAG, "get ip address - err");
        return "";
    }

    //token 및 deviceId를 저장
    private static void saveSpData(String label, String data, Activity activity){
        SharedPreferences sp = activity.getSharedPreferences("token", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(label, data).apply();
    }

    private static String getSpData(String label, Activity activity){
        return activity.getSharedPreferences("token",0).getString(label,"");
    }

    public static void enrollDevice(Activity activity){
        RetrofitManager rm =SharedData.getManager();

        rm.enrollDevice(in_uuid,
                "M",
                in_os,
                in_device,
                GetLocalIpAddress(),
                mac,
                it -> {
                    // Device Enroll Suc - not yey
                    if (it.get("msg").equals("200")) {
                        //Null check token and device
                        if (it.get("token").isEmpty() || it.get("device_id").isEmpty()) {
                            Log.e(TAG, "shared data - enrollDevice() Token is null");
                        }
                        // Device Enroll Suc - complete
                        else {
                            Log.e(TAG, "shared data - enrollDevice() Success get Token & Id");
                            //전역 변수로 저장
                            token = it.get("token");
                            device_id = it.get("device_id");
                            //token 및 deviceId를 저장
                            saveSpData("token", token, activity);
                            saveSpData("device_id", device_id, activity);

                            // 처음 등록 후, 접근 권한이 있는지 다시 한번 확인하는 용도
                            if(!counter){
                                counter = true;
                                enrollDevice(activity);
                            }
                        }
                    }
                    else if(it.get("msg").equals("403")){
                        Log.e(TAG, "접근 거부");
                        ((ProcessListener)activity).state_result(State.NoPermissionToAccess);
                    }
                    else if(it.get("msg").equals("409")){
                        token = getSpData("token", activity);
                        device_id = getSpData("device_id", activity);
                        Log.e(TAG, "앱 삭제 후, 토큰 null");

                        if(token=="" || device_id=="") ((ProcessListener)activity).state_result(State.NoPermissionToAccess);
                    }
                    // Device Enroll Err or Fail
                    else {
                        Log.e(TAG, "shared data - enrollDevice() 통신오류");
                        SharedData.sendMessage(getNetworkErrorCode(it.get("msg")));
                        ((ProcessListener)activity).liveness_result(getNetworkErrorCode(it.get("msg")));
                    }

                    return null;
                });
//        }
    }

    // 결과 코드 서버 전송
    public static void sendMessage(State stateMsg){
        String key=stateMsg.getMsg().get("code");

        String _code;
        String value;

        assert key != null;
        String[] temp = key.split("-");
        _code = temp[0];
        value = temp[1];
//        Log.e("Test>>",token+"\n"+in_userId+"\n"+device_id+"\n"+_code+"\n"+value);

        //device_id -> userId
        if(token!=null)
            retrofitManager.upHistory(token, device_id,_code,value);
    }

    public static State getNetworkErrorCode(String stateMsg){
        State result;
        switch (stateMsg) {
            case "Check the Internet!":
                result = State.DeviceNoNetwork;
                break;
            case "401":
                result = State.Server401;
                break;
            case "403":
                result = State.Server403;
                break;
            case "404":
                result = State.Server404;
                break;
            case "400":
                result = State.Server400;
                break;
            case "500":
                result = State.Server500;
                break;
            case "503":
                result = State.Server503;
                break;
            default:
                Log.e("CUBOX","getNetworkErrorCode-unknown err");
                result = State.Server500;
                break;
        }

        return result;
    }
}