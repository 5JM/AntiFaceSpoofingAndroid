package aero.cubox.icheckerpassive;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;

import java.util.Vector;

import aero.cubox.communication.ProcessListener;
import aero.cubox.communication.SharedData;
import aero.cubox.communication.utils.State;
import aero.cubox.facedetector.FaceVerification;
import aero.cubox.liveness.PassiveLiveness;

public class EstimateActivity extends AppCompatActivity implements ProcessListener {
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "CUBOX";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private PreviewView preview;
    private ProgressBar progressbar;
    private PassiveLiveness passiveLiveness;
    private FaceVerification faceVerification;

    private Dialog dialog;
    private Button estiButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_estimate);

        initComponents();

        initVector();

        showDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, ">> Camera permission not granted");
            ActivityCompat.requestPermissions(EstimateActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
            return;
        }
        // 필수
        if(passiveLiveness!=null) {
            passiveLiveness.release();
            passiveLiveness = null;
        }
        SharedData.duringEst = false;
        passiveLiveness = new PassiveLiveness(this, preview, progressbar);
        passiveLiveness.settingCamera(true, true);

        faceVerification = new FaceVerification(this, preview);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 필수
        if(passiveLiveness!=null){
            passiveLiveness.release();
            passiveLiveness = null;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        dialog.dismiss();
        // 필수
        if(passiveLiveness!=null){
            passiveLiveness.release();
            passiveLiveness = null;
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // 필수
        if(passiveLiveness!=null)
            passiveLiveness.release();
        startActivity(new Intent(EstimateActivity.this, MainActivity.class));
        finish();
    }
    /***********************************************************************************************
     *                                         Permission
     **********************************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!allPermissionsGranted(grantResults)) {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("종료");
            builder.setMessage("이 앱은 카메라기능이 필수입니다.");
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    moveTaskToBack(true); // 태스크를 백그라운드로 이동
                    finishAndRemoveTask(); // 액티비티 종료 + 태스크 리스트에서 지우기
                    System.exit(0);
                }
            });
            AlertDialog alertDialog =builder.create();
            alertDialog.show();
        }
        requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE
        }, PERMISSION_REQUEST_CODE);
    }
    // real face 일때만, 통신
    @Override
    public void liveness_result(State state) {
        if ("RealFace".equals(state.getMsg().get("code"))) {
            faceVerification.Verification1Start();
        } else {
            faceVerification.TampFail(state);

            Intent intent = new Intent(EstimateActivity.this, ResultActivity.class);
            intent.putExtra("guide", state.getMsg().get("guide"));
            intent.putExtra("msg", state.getMsg().get("msg"));
            startActivity(intent);
            finish();
        }
    }

    // real face 이외에 모든 처리는 아래 함수로
    @Override
    public void state_result(State state) {
        // 필수
        if(passiveLiveness!=null){
            passiveLiveness.release();
            passiveLiveness = null;
        }
        Intent intent = new Intent(EstimateActivity.this, ResultActivity.class);
        intent.putExtra("guide",state.getMsg().get("guide"));
        intent.putExtra("msg",state.getMsg().get("msg"));
        startActivity(intent);
        finish();
    }

    private void initComponents(){
        progressbar = findViewById(R.id.cubox_progressbar);
        progressbar.setProgress(0);

        preview = findViewById(R.id.preview);
    }

    private void showDialog(){
        setInitDialog();

        estiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Liveness 시작
                Boolean check = passiveLiveness.livenessStart();
                dialog.dismiss();

                if(!check){
                    setReDialog();
                }
            }
        });
    }

    // 측정 버튼 다이얼로그
    private void setInitDialog(){
        dialog = new Dialog(EstimateActivity.this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);

        // dialog 배경 및 뒤로가기 터치 막기
        dialog.setCancelable(false);

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        estiButton = dialog.findViewById(R.id.dialog_button);
    }

    // 측정 버튼 다이얼로그 다시 띄우기
    // - 다시 다이얼로그 띄울때는 liveness를 다시 시작해야함으로 나눠놈
    private void setReDialog(){
        dialog = new Dialog(EstimateActivity.this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fail_dialog);

        // dialog 배경 및 뒤로가기 터치 막기
        dialog.setCancelable(false);

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        estiButton = dialog.findViewById(R.id.dialog_restart_button);
        estiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Liveness 시작
                Boolean check = passiveLiveness.livenessStart();
                dialog.dismiss();
                if(!check) setReDialog();
            }
        });
    }

    private void initVector(){
        SharedData.thrsArr.removeAllElements();
        SharedData.thrs.removeAllElements();

        Vector<Float> emptyTemp = new Vector<>();
        emptyTemp.add(0f);
        emptyTemp.add(0f);
        emptyTemp.add(0f);
        emptyTemp.add(0f);

        SharedData.thrsArr = emptyTemp;
        SharedData.thrs = emptyTemp;
    }
}