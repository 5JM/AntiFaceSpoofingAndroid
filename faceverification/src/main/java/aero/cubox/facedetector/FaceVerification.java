package aero.cubox.facedetector;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;

import androidx.camera.view.PreviewView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import aero.cubox.communication.CameraX;
import aero.cubox.communication.FaceDetector;
import aero.cubox.communication.ProcessListener;
import aero.cubox.communication.SharedData;
import aero.cubox.communication.utils.State;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class FaceVerification {
    private Activity activity;
    private CameraX cameraInterface;
    private static State state;
    public FaceVerification(Activity _activity, PreviewView preview){
        this.activity = _activity;

        cameraInterface = new CameraX(activity, preview);
    }

    // Fake face 일때, 처리 결과 서버 전송
    public void TampFail(State state){
        //image들을 담을 byte_array
        byte[] id_byte_array = new byte[0];
        byte[] live_byte_array = new byte[0];

        try {
            id_byte_array = BitmapToByteArray(SharedData.getIdBitmap());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CUBOX","id_byte_array exception");
        }

        try {
            live_byte_array = BitmapToByteArray(SharedData.first_live_img);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CUBOX","live_byte_array exception");
        }

        //image들을 통신을 위해 비트맵에서 MultipartBody로 변환
        RequestBody live_request_file = RequestBody.create(MediaType.parse("multipart/form-data"), live_byte_array);
        RequestBody id_request_file = RequestBody.create(MediaType.parse("multipart/form-data"), id_byte_array);

        // #param name : image1, image2로 되어있는건 서버쪽에서 이름을 이렇게 지정해놔서 따른것
        // 헷갈리거나 변경하려면 서버쪽과 상의.
        MultipartBody.Part liveImage = MultipartBody.Part.createFormData("Image2", "liveimage", live_request_file);
        MultipartBody.Part idImage = MultipartBody.Part.createFormData("Image1", "idimage", id_request_file);

        Vector<Float> thrs = SharedData.thrsArr; // live
        Vector<Float> thrsServer = SharedData.thrs; // server thr

        String[] temp = state.getMsg().get("code").split("-");
        String _code = temp[0];
        String _value = temp[1];

        MultipartBody.Part score1 = MultipartBody.Part.createFormData("Score1",  thrs.get(0).toString());
        MultipartBody.Part score2 = MultipartBody.Part.createFormData("Score2",  thrs.get(1).toString());
        MultipartBody.Part score3 = MultipartBody.Part.createFormData("Score3",  thrs.get(2).toString());
        MultipartBody.Part score4 = MultipartBody.Part.createFormData("Score4",  thrs.get(3).toString());

        MultipartBody.Part thr1 = MultipartBody.Part.createFormData("Threshold1",  thrsServer.get(0).toString());
        MultipartBody.Part thr2 = MultipartBody.Part.createFormData("Threshold2",  thrsServer.get(1).toString());
        MultipartBody.Part thr3 = MultipartBody.Part.createFormData("Threshold3",  thrsServer.get(2).toString());
        MultipartBody.Part thr4 = MultipartBody.Part.createFormData("Threshold4",  thrsServer.get(3).toString());

        MultipartBody.Part code = MultipartBody.Part.createFormData("ErrorCode",  _code);
        MultipartBody.Part value = MultipartBody.Part.createFormData("ErrorValue",  _value);

        SharedData.getManager().tamperFail(
                SharedData.token, liveImage, idImage,
                score1, score2, score3, score4,
                thr1, thr2, thr3, thr4,
                code, value
        );
    }

    // 1:1 인증
    public void Verification1Start(){
        Log.e("CUBOX","start Verification");
        //image들을 담을 byte_array
        byte[] id_byte_array = new byte[0];
        byte[] live_byte_array = new byte[0];

        try {
            id_byte_array = BitmapToByteArray(SharedData.getIdBitmap());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CUBOX","id_byte_array exception");
        }

        try {
            live_byte_array = BitmapToByteArray(SharedData.first_live_img);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CUBOX","live_byte_array exception");
        }

        //image들을 통신을 위해 비트맵에서 MultipartBody로 변환
        RequestBody live_request_file = RequestBody.create(MediaType.parse("multipart/form-data"), live_byte_array);
        RequestBody id_request_file = RequestBody.create(MediaType.parse("multipart/form-data"), id_byte_array);

        // #param name : image1, image2로 되어있는건 서버쪽에서 이름을 이렇게 지정해놔서 따른것
        // 헷갈리거나 변경하려면 서버쪽과 상의.
        MultipartBody.Part liveImage = MultipartBody.Part.createFormData("Image2", "liveimage", live_request_file);
        MultipartBody.Part idImage = MultipartBody.Part.createFormData("Image1", "idimage", id_request_file);

        SharedData.getManager().faceVerification(
                SharedData.token, liveImage, idImage,
                it ->{
                    if (it.get("msg").equals("200")) {
                        if (it.get("match").equals("true")) state = State.Success;
                        else state = State.NotMatch;
                    }
                    else{
                        state = SharedData.getNetworkErrorCode(it.get("msg"));
                    }
                    SharedData.sendMessage(state);
                    resultState(state);
                    return null;
                });
    }

    public void release(){
        if(cameraInterface!=null) {
            cameraInterface = null;
        }
        if(SharedData.retrofitManager!=null) SharedData.retrofitManager = null;
    }

    //통신을 위해 비트맵을 바이트 배열로 바꿔주는 함수
    private byte[] BitmapToByteArray(Bitmap bitmap) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
        byte[] resultByteArr = byteStream.toByteArray();

        byteStream.close();

        return resultByteArr;
    }

    private void resultState(State state){
        // state code
        ((ProcessListener)activity).state_result(state);
    }
}
