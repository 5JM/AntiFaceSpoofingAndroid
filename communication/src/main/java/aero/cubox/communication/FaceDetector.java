package aero.cubox.communication;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import aero.cubox.communication.utils.State;

public class FaceDetector {
    private static final String TAG = "FaceDetector";

    private final Activity activity;
    // Face detector
    private Rect bbox = new Rect(0, 0, 0, 0);
    private Detector face_detector = null;

    //얼굴 감지 안하면 자동 종료관련 변수
    // Face detector
    private final long DETECTION_TIMER_INTERVAL = 100L;
    public static Timer detectionTimer;
    private Long faceDetectTime ;

    public FaceDetector(Activity activity) {
        this.activity = activity;
        // Start face detection
        start_face_detect_thread();
    }

    /***********************************************************************************************
     *                                         Face detection
     **********************************************************************************************/
    private void start_face_detect_thread() {
        if (face_detector == null) {
            // 얼굴 검출 관련
            String assetPath = assetFilePath(activity, "slim_320_without_postprocessing.onnx");
            String model_path = new File(assetPath).getAbsolutePath();
            //////don't edit this value ( in_height & in_width) //////
            int in_height = 320;
            int in_width = 240;

            float score_thresh = 0.8f;
            float iou_thresh = 0.5f;
            face_detector = new Detector(model_path, "", in_width, in_height, score_thresh, iou_thresh, true);
        }

        faceDetectTime = System.currentTimeMillis();
        detectionTimer = new Timer();
        detectionTimer.schedule(new DetectionTimer(), 1000, DETECTION_TIMER_INTERVAL);
    }

    public static void stop_face_detect_thread() {
        if (detectionTimer != null) {
            detectionTimer.cancel();
            detectionTimer = null;

        }
    }

    private void detect_face() {
        Bitmap bitmap= SharedData.getBitmap();
        if (bitmap != null) {
            bbox = face_detector.run(bitmap);
        }
    }

    //onnx 파일 사용하기 위함
    public String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error process asset " + assetName + " to file path");
        }
        return null;
    }

    private class DetectionTimer extends TimerTask {
        int count = 0;
        @Override
        public void run() {
            detect_face();
            long temp = System.currentTimeMillis() - faceDetectTime;
            //5초씩 3번 총 15초간 얼굴을 감지하지 못하면 앱 종료
            try{
                switch (count) {
                    case 0: {
                        if(bbox.width() > 1){
                            count = 0;
                            faceDetectTime = System.currentTimeMillis();
                        }
                        else{
                            if(temp>=5000) count=1;
                        }
                        break;
                    }
                    case 1: {
                        if(bbox.width() > 1){
                            count = 0;
                            faceDetectTime = System.currentTimeMillis();
                        }
                        else{
                            if(temp>=10000) count=2;
                        }
                        break;
                    }
                    default: {
                        if(bbox.width() > 1){
                            count = 0;
                            faceDetectTime = System.currentTimeMillis();
                        }
                        else{
                            if(temp>=16000){
                                Log.e(TAG,"Time over.caused by No face");
                                super.cancel();

                                release();

                                ((ProcessListener)activity).liveness_result(State.NoFace);
                            }
                        }
                        break;
                    }
                }
            }catch (Exception e){
                Log.e(TAG, "Error Live Face Detect...");
                super.cancel();
                e.printStackTrace();
                //stop face detect and signal processing
                release();

                ((ProcessListener)activity).liveness_result(State.NoFace);
            }
        }
    }

    public Rect getFaceBox(){ return bbox; }

    public void release() {
        stop_face_detect_thread();
    }
}
