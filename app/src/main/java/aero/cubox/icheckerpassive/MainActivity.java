package aero.cubox.icheckerpassive;

import androidx.annotation.ColorRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import aero.cubox.communication.*;
import aero.cubox.communication.utils.State;
import aero.cubox.liveness.PassiveLiveness;

public class MainActivity extends AppCompatActivity implements ProcessListener {
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "CUBOX";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Boolean lens = true;

    private PreviewView preview;
    //    private CustomPreview preview;
    private TextView guidText;
    private ImageView changeServer;

    private Button takePictureButton;
    private ImageButton changeLensButton;
    private ImageView idCardView;
    private ImageView container;
    private float containerWidth ;
    private PassiveLiveness passiveLiveness;

    private Boolean result = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initComponents();

        initVector();

        changeTextColor(guidText);

        //Liveness모듈 호출
//            passiveLiveness = new PassiveLiveness(this, preview, takePictureButton);

        // 접근 권한 초기화 및 체크
        SharedData.counter = false;
        SharedData.firstFlag = true;
        SharedData.createManager(this);

        //촬영 버튼
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fadeInAnim(idCardView, changeLensButton, takePictureButton);

                Timer timer = new Timer();
                TimerTask TT = new TimerTask() {
                    @Override
                    public void run() {
                        // 반복실행할 구문
                        result = passiveLiveness.takePicture();

                        if(result) {
                            timer.cancel();

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override public void run() {
                                    startActivity(new Intent(MainActivity.this, GetIdImageActivity.class));
                                    finish();
                                }
                            }, 1000);
                        }
                    }
                };
                timer.schedule(TT, 0, 1000); //Timer 실행
            }
        });

        changeLensButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 모듈 초기화
                passiveLiveness.resetFaceDetector();
                lens = !lens;
                passiveLiveness.settingCamera(lens, false);
            }
        });

        SharedData.versionFlagT.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    SharedData.setUrl(BuildConfig.BASE_URL_V1);
                    setTint(changeServer, R.color.cubox_sky);
                }
                //v2
                else {
                    SharedData.setUrl(BuildConfig.BASE_URL);
                    setTint(changeServer, R.color.white);
                }
            }
        });

        changeServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedData.versionFlagT.postValue(!SharedData.versionFlagT.getValue());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, ">> Camera permission not granted");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
            return;
        }
        // 필수
        if(passiveLiveness!=null) {
            passiveLiveness.release();
            passiveLiveness = null;
        }
        SharedData.duringEst = false;
//        passiveLiveness = new PassiveLiveness(this, preview, takePictureButton);
        passiveLiveness = new PassiveLiveness(this, preview);
        passiveLiveness.settingCamera(lens, false);
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
    public void onBackPressed() {
        super.onBackPressed();

        // 필수
        if(passiveLiveness!=null)
            passiveLiveness.release();

        MainActivity.this.finish();
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        containerWidth = (float) takePictureButton.getRight() - (float)takePictureButton.getWidth()/2 - (float) container.getWidth()/2;
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

    private void initComponents(){
        preview = findViewById(R.id.preview);
        guidText = findViewById(R.id.cubox_title);
        takePictureButton = findViewById(R.id.take_picture_button);
        changeLensButton = findViewById(R.id.change_lens_button);
        changeServer = findViewById(R.id.title);
        idCardView = findViewById(R.id.id_card_view);
        container = findViewById(R.id.round_box);
    }
    private void changeTextColor(TextView textView){
        // SpannableStringBuilder로 text를 설정
        SpannableStringBuilder sp = new SpannableStringBuilder(textView.getText());
        // SpannableStringBuilder의 원하는 부분을 지정하여 글자색 변경
        sp.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.cubox_sky))
                , 0
                , 3
                , Spannable.SPAN_INTERMEDIATE
        );
        // 해당내용 적용
        textView.setText(sp);
    }
    private void setTint(ImageView view, @ColorRes int color){
        Drawable drawable = DrawableCompat.wrap(view.getDrawable());

        DrawableCompat.setTint(
                drawable.mutate(),
                ContextCompat.getColor(this, color)
        );
    }

    @Override
    public void state_result(State state) {
        if(state == State.NoPermissionToAccess){
            Intent intent = new Intent(this, NoAccessActivity.class);
            intent.putExtra("msg",state.getMsg().get("msg"));
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void liveness_result(State state) {
        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putExtra("guide",state.getMsg().get("guide"));
        intent.putExtra("msg",state.getMsg().get("msg"));
        startActivity(intent);
        finish();
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

    private void fadeInAnim(ImageView idCardView, ImageView changeLens, Button takePicture){
        // animation 효과 여러개 적용시
        PropertyValuesHolder scaleX_D = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f);
        PropertyValuesHolder scaleX_U = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -containerWidth);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.25f, 0.3f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.25f, 0.3f);

        ObjectAnimator idAnimation = ObjectAnimator.ofPropertyValuesHolder(idCardView, scaleX,scaleY);
        idAnimation.setDuration(700);
        idAnimation.setInterpolator(new OvershootInterpolator());
        idAnimation.start();

        idCardView.setImageResource(R.drawable.id_card_icon2);

        ObjectAnimator lensAnimation = ObjectAnimator.ofPropertyValuesHolder(changeLens, scaleX_D);
        lensAnimation.setDuration(300);
        lensAnimation.setInterpolator(new AccelerateInterpolator());
        lensAnimation.start();

        ObjectAnimator pictureAnimation = ObjectAnimator.ofPropertyValuesHolder(takePicture, scaleX_U);
        pictureAnimation.setDuration(300);
        pictureAnimation.setInterpolator(new AccelerateInterpolator());
        pictureAnimation.start();

        takePicture.setEnabled(false);
        takePicture.setText("자동으로 촬영됩니다.");
        takePicture.setTextColor(Color.WHITE);
    }

    private void fadeOutAnim(ImageView idCardView, ImageView changeLens, Button takePicture){
        // animation 효과 여러개 적용시
        PropertyValuesHolder scaleX_D = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f);
        PropertyValuesHolder scaleX_U = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, containerWidth);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.25f, 0.3f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.25f, 0.3f);

        ObjectAnimator idAnimation = ObjectAnimator.ofPropertyValuesHolder(idCardView, scaleX,scaleY);
        idAnimation.setDuration(700);
        idAnimation.setInterpolator(new OvershootInterpolator());
        idAnimation.start();

        idCardView.setImageResource(R.drawable.id_card_icon1);

        ObjectAnimator lensAnimation = ObjectAnimator.ofPropertyValuesHolder(changeLens, scaleX_D);
        lensAnimation.setDuration(300);
        lensAnimation.setInterpolator(new AccelerateInterpolator());
        lensAnimation.start();

        ObjectAnimator pictureAnimation = ObjectAnimator.ofPropertyValuesHolder(takePicture, scaleX_U);
        pictureAnimation.setDuration(300);
        pictureAnimation.setInterpolator(new AccelerateInterpolator());
        pictureAnimation.start();

    }
}