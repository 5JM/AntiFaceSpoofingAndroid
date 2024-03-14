package aero.cubox.liveness;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import aero.cubox.communication.*;
import aero.cubox.communication.utils.*;

import static aero.cubox.communication.FaceDetector.stop_face_detect_thread;

public class Liveness {
    private static final String TAG = "Liveness";
    private final Activity activity;
    //    private static float t1 = 0.09f, t2 = 0.11f, t3 = 0.06f, t4 = 0.08f;
    private static float t1 = 0.098f, t2 = 0.119f, t3 = 0.075f, t4 = 0.078f;
    private Estimator estimator = null;
    private HandlerThread signal_thread;
    private Handler signal_handler;
    private Boolean runFlag = true;
    private long lastTime = 0;
    private Rect bbox = null;
    private ProgressBar progressBar;

    private long est_start = -1;

    private Boolean firstFaceImgFlag = false;
    private Bitmap firstFaceImg;

    private int randomSeed;

    protected Liveness(Activity activity, Rect faceBox, ProgressBar progressBar){
        this.activity = activity;
        this.bbox = faceBox;
        this.progressBar = progressBar;

        this.randomSeed = (int) (Math.random() * 5000);

        // create Retrofit Manager
        SharedData.createManager(activity);

        // 서버로부터 thresh hold값을 가져옴.
        SharedData.getManager().getThreshHold(
                it->{
                    Log.e(TAG, "th return : "+it);
                    //threshold 조정
                    try{
                        setThreshold(
                                Float.parseFloat(Objects.requireNonNull(it.get("00002"))),
                                Float.parseFloat(Objects.requireNonNull(it.get("00003"))),
                                Float.parseFloat(Objects.requireNonNull(it.get("00004"))),
                                Float.parseFloat(Objects.requireNonNull(it.get("00005")))
                        );
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.e("Parse>>","can't parse");
                    }
                    return null;
                }
        );

        final int fps = 30;
        final float duration = 5.0f;
        final int minBpm = 40;
        final int maxBpm = 180;
        final float bpmUpdateInterval = 1.0f;

        // cpp liveness 연결
        estimator = new Estimator(fps, duration, minBpm, maxBpm, bpmUpdateInterval);
        // liveness 측정 시작 thread
        start_signal_process_thread();
    }

    /***********************************************************************************************
     *                                     Processing (Signal)
     **********************************************************************************************/
    private void start_signal_process_thread(){
        if (estimator != null){
            estimator.reset();
        }

        // input parameters :
        // in_userId, in_device, in_os, in_uuid
        // 바로시작하는걸 방지
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                signal_thread = new HandlerThread("Signal");
                signal_thread.start();
                signal_handler = new Handler(signal_thread.getLooper());
                signal_handler.post(periodic_signal_processing);
            }},1000);
    }

    private void stop_signal_process_thread() {
        if (signal_thread != null) {
            signal_thread.quitSafely();
            try {
                SharedData.startEstFlag=false;
                signal_thread.join();
                signal_thread = null;
                signal_handler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private final Runnable periodic_signal_processing;{
        periodic_signal_processing = new Runnable() {
            @Override
            public void run() {
                try{
                    while(runFlag){
                        // 동시 접근 제한
                        if (SharedData.use_liveBitmap){
                            SharedData.use_liveBitmap = false;
                            boolean ret = false;
                            //버튼 누르면 processing시작
                            if(SharedData.startEstFlag) ret = processing();

                            if (ret) break;
                        }
                    }
                }catch (Exception e ){
                    Log.e("ThreadTest>>","except signal process!!");
                    e.printStackTrace();
                }finally {
                    Log.e("ThreadTest>>","stop signal process!!");
                }
            }
        };
    }
    // liveness 결과에 따른 프로세스 함수
    private boolean processing() {
        adjustFPS(System.currentTimeMillis());
        //검출된 얼굴이 있을때만 수행
        if (bbox.width() > 1 && bbox.height()>1) {
            // 처음 시작할때
            if (est_start == -1){
                stop_face_detect_thread();
                est_start = System.currentTimeMillis();
            }
            // 서버에 업로드할 live 이미지 저장
            if(!firstFaceImgFlag){
                SharedData.first_live_img = SharedData.getBitmap();
                firstFaceImg = SharedData.getBitmap();
                firstFaceImgFlag = true;
            }
            // live 이미지를 가져와 얼굴만 자름
            Bitmap cropped = Bitmap.createBitmap(SharedData.getBitmap(), bbox.left, bbox.top, bbox.width(), (int) (bbox.height() * 0.7));
            // processing
            estimator.processFrame(cropped, System.currentTimeMillis(), false);

            long est_end = System.currentTimeMillis();
            long dif = est_end - est_start;
            // 진행률
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress( (int)((float)dif / 50));
                }
            });

            // 서버에 올릴 live img를 random하게 저장
            if(Math.abs(randomSeed-dif)<=20) SharedData.first_live_img = SharedData.getBitmap();

            // 5초간 frame을 받음
            if (dif >= 5000) {
                //임계값 확인용 벡터 초기화
                SharedData.thrsArr.removeAllElements();
                // liveness 결과
                int is_real = estimator.predict(t1, t2, t3, t4, SharedData.thrsArr); //임계값을 넘겨준 후, 측정 시작

                State state;
                //  real face = 0
                //  fake face = 1
                //  movement and light change = 2
                if (is_real==0) {
                    Log.e(TAG,"real face");
                    state = State.RealFace;
                }
                else if(is_real==1){
                    Log.e(TAG,"Not real face");
                    state = State.NotRealFace;
                    SharedData.sendMessage(state);
                }else if(is_real==2){
                    Log.e(TAG,"Movement & Light change");
                    state = State.MovementLightChange;
                    SharedData.sendMessage(state);
                }
                else {
                    Log.e(TAG,"Image size err");
                    state = State.InternalErr;
                    SharedData.sendMessage(state);
                }
                resultState(state);
                return true;
            }

        }
        return false;
    }

    private void adjustFPS(Long currentTime) {
        long loopTime = currentTime - lastTime;
        lastTime = currentTime;

        // 1장당 걸리는 시간
        long frameTime = 33l;
        long sleepTime = frameTime - loopTime;
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setThreshold(float _t1,float _t2,float _t3,float _t4) {
        t1 = _t1;
        t2 = _t2;
        t3 = _t3;
        t4 = _t4;

        SharedData.thrs.removeAllElements();
        Vector<Float> temp = new Vector<>();
        temp.add(_t1);
        temp.add(_t2);
        temp.add(_t3);
        temp.add(_t4);

        SharedData.thrs = temp;
    }

    public void release() {
        runFlag = false;
//        stop_face_detect_thread();
        stop_signal_process_thread();
    }

    protected Bitmap getLivenessResultImg(){
        return firstFaceImg;
    }

    private void resultState(State state){
        ((ProcessListener)activity).liveness_result(state);
    }
}
