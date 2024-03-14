package aero.cubox.liveness;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.camera.view.PreviewView;

import aero.cubox.communication.*;

public class PassiveLiveness {
    private Activity activity;
    private CameraX cameraInterface;
    private FaceDetector faceDetector;
    private Rect bbox;
    private ProgressBar progressBar;
    private Liveness liveness;

    // 신분증 촬영시 사용하는 생성자 - 프로그래스바 필요 x
    public PassiveLiveness(Activity activity, PreviewView preview){
        this.activity = activity;
        cameraInterface = new CameraX(activity, preview);
        faceDetector = new FaceDetector(activity);
    }
    // 라이브니스 측정시 사용하는 생성자 - 프로그래스바 필요
    public PassiveLiveness(Activity activity, PreviewView preview, ProgressBar _progress){
        this.activity = activity;
        this.progressBar = _progress;
        cameraInterface = new CameraX(activity, preview);
        faceDetector = new FaceDetector(activity);
    }

    // Liveness를 호출하며 측정 시작
    public Boolean livenessStart(){
        bbox = faceDetector.getFaceBox();
        if(bbox.width()>1 && bbox.height()>1){
            liveness = new Liveness(activity, bbox, progressBar);
            SharedData.duringEst = true;
            SharedData.startEstFlag = true;
            return true;
        }
        else
            return false;
    }

    public void resetFaceDetector(){
        faceDetector.release();
        faceDetector = null;
        faceDetector = new FaceDetector(activity);
    }

    // liveness에서 사용된 이미지 얻는 함수
    public Bitmap getLivenessImg(Boolean cropped){
        if(cropped)  return Bitmap.createBitmap(
                liveness.getLivenessResultImg(),
                bbox.left,
                bbox.top,
                bbox.width(),
                bbox.height()
        );
        else return liveness.getLivenessResultImg();
    }

    // 카메라 설정
    public void settingCamera(Boolean lensState, Boolean estimateFlag){
        if(cameraInterface!=null) cameraInterface.startCamera(lensState, estimateFlag);
    }

    // 신분증 이미지 촬영
    public Boolean takePicture(){
        if(cameraInterface!=null) {
            if (faceDetector.getFaceBox().width()>1 && faceDetector.getFaceBox().height()>1){
                cameraInterface.takePicture();
                return true;
            }
        }
        return false;
    }

    public void release(){
        if(liveness != null){
            liveness.release();
            liveness=null;
        }
        if(faceDetector != null) {
            faceDetector.release();
            faceDetector=null;
        }
        if(cameraInterface!=null) {
//            cameraInterface.release();
            cameraInterface = null;
        }

        if(SharedData.retrofitManager!=null) SharedData.retrofitManager = null;
    }
}