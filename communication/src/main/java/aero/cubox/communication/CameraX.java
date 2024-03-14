package aero.cubox.communication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import aero.cubox.communication.utils.State;

public class CameraX {
    private Activity activity;
    private PreviewView viewFinder;
    private ImageCapture imageCapture;

    public CameraX(Activity _activity, PreviewView _preview){
        this.activity = _activity;
        this.viewFinder = _preview;
    }

    public void takePicture(){
        if(imageCapture!=null){
            imageCapture.takePicture(
                    Executors.newSingleThreadExecutor(),
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            super.onCaptureSuccess(image);

                            int rot = image.getImageInfo().getRotationDegrees();
                            Bitmap _t = toBitmapForCapture(image);

                            SharedData.setIdBitmap(Bitmap.createBitmap(_t, 0, 0, _t.getWidth(), _t.getHeight(), getRotation(rot),false));
//                            SharedData.setIdBitmap(_t);
                            _t.recycle();

                            image.close();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                        }
                    });
        }
    }

    public void startCamera(Boolean lensState, Boolean estimateFlag){
        // full screen
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar

        uiFlags |= 0x00001000; // SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide
        activity.getWindow().getDecorView().setSystemUiVisibility(uiFlags);

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);

        cameraProviderFuture.addListener(new Runnable() {
            @SuppressLint("UnsafeOptInUsageError")
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    Preview preview = new Preview.Builder().build();
                    CameraSelector cameraSelector;
                    // 전면 후면 카메라 설정
                    if(lensState){
                        cameraSelector = new CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                                .build();
                    }else {
                        cameraSelector = new CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build();
                    }

                    ImageAnalysis imageAnalysis;

                    imageAnalysis =
                            new ImageAnalysis.Builder()
                                    //set the resolution of the view
//                              .setTargetResolution(new Size(preview.getResolutionInfo().getResolution().getWidth(),preview.getResolutionInfo().getResolution().getHeight()))
                                    //the executor receives the last available frame from the camera at the time that the analyze() method is called
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                              .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
//                              .setImageQueueDepth()
                                    .build();
                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new ImageAnalysis.Analyzer() {
                        Bitmap retimage;
                        @Override
                        public void analyze(@NonNull ImageProxy image) {
                            //Convert Yuv to jpeg
                            retimage = toBitmap(image);
                            int rot = image.getImageInfo().getRotationDegrees();
                            Bitmap bitmap = Bitmap.createBitmap(retimage, 0, 0, retimage.getWidth(), retimage.getHeight(), getRotation(rot),false);

                            SharedData.setLiveBitmap(bitmap);
                            retimage.recycle();

                            // released stream
                            image.close();
                        }
                    });

                    ImageCapture.Builder builder = new ImageCapture.Builder();
                    @SuppressLint("UnsafeOptInUsageError")
                    Camera2Interop.Extender<ImageCapture> extender = new Camera2Interop.Extender<>(builder);

                    // 자동 노출 및 화이트 밸런스 off
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override public void run() {
                            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true);
                            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true);
                        }
                    }, 1000);

                    // 신분증 촬영/캡쳐 시 사용
                    if(!estimateFlag){
                        imageCapture = new ImageCapture.Builder()
                                .build();
                    }

                    //프리뷰 centerCrop
                    viewFinder.setScaleType(PreviewView.ScaleType.FILL_CENTER);

                    // Connect the preview use case to the previewView
                    preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                    cameraProvider.unbindAll();
                    // Attach use cases to the camera with the same lifecycle owner
                    Camera camera;
                    // liveness 측정용
                    if(estimateFlag){
                        camera = cameraProvider.bindToLifecycle(
                                ((LifecycleOwner)activity),
                                cameraSelector,
                                preview,
                                imageAnalysis
                        );
                    }
                    // 이미지 캡쳐용
                    else {
                        camera = cameraProvider.bindToLifecycle(
                                ((LifecycleOwner)activity),
                                cameraSelector,
                                preview,
                                imageAnalysis,
                                imageCapture
                        );
                    }
                    // touch focus
                    CameraControl cameraControl = camera.getCameraControl();
                    // lock AF
//                    cameraControl.cancelFocusAndMetering();
                    viewFinder.setOnTouchListener(new View.OnTouchListener() {
                        Boolean result;
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            switch (motionEvent.getAction()){
                                case MotionEvent.ACTION_DOWN:
                                    view.performClick();
                                    result = true;
                                    break;

                                case MotionEvent.ACTION_UP:
                                    MeteringPointFactory factory = viewFinder.getMeteringPointFactory();
                                    MeteringPoint point = factory.createPoint(motionEvent.getX(),motionEvent.getY());
                                    FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                            .disableAutoCancel().build();
                                    cameraControl.startFocusAndMetering(action);
                                    view.performClick();
                                    result = true;
//                                    Log.e("Focus>>",""+motionEvent.getX()+", "+motionEvent.getY());
                                    break;
                                default:
                                    result = false;
                                    break;
                            }
                            return result;
                        }
                    });
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                    Log.e("CameraInterface", "openCamera: "+e);
                    Log.e("Open Camera", "카메라를 사용할 수 없습니다.");

                    ((ProcessListener)activity).liveness_result(State.DeviceNoCamera );
                }
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    // 회전방향 구하기
    private Matrix getRotation(int rotation){
        Matrix rotate_matrix = new Matrix();
        rotate_matrix.postRotate(rotation);
        return rotate_matrix;
    }

    // live img -> take picture에 사용하면 죽음...
    private Bitmap toBitmap(ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    // id img
    private Bitmap toBitmapForCapture(ImageProxy image){
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        byte[] clonedBytes = bytes.clone();

        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);
    }
}