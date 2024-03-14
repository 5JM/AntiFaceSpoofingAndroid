package aero.cubox.icheckerpassive;

import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;

import aero.cubox.communication.SharedData;

public class GetIdImageActivity extends AppCompatActivity {
    private ImageView idPreview;
    private Button reTakeButton;
    private Button useImgButton;
    private TextView guidText;
    private ImageView changeServer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_id_image);

        initComponents();
        changeTextColor(guidText);
        setImageView();

        // full screen
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar

        uiFlags |= 0x00001000; // SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);


        if(SharedData.versionFlagT.getValue()){
            setTint(changeServer, R.color.cubox_sky);
        }else{
            setTint(changeServer, R.color.white);
        }

        reTakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GetIdImageActivity.this, MainActivity.class));
                finish();
            }
        });

        useImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GetIdImageActivity.this, EstimateActivity.class));
                finish();
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
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(GetIdImageActivity.this, MainActivity.class));
        finish();
    }

    private void initComponents(){
        idPreview = findViewById(R.id.id_img);
        reTakeButton = findViewById(R.id.retake_button);
        useImgButton = findViewById(R.id.use_button);
        guidText = findViewById(R.id.cubox_title);
        changeServer = findViewById(R.id.title);
    }

    private void changeTextColor(TextView textView){
        // SpannableStringBuilder로 text를 설정
        SpannableStringBuilder sp = new SpannableStringBuilder(textView.getText());
        // SpannableStringBuilder의 원하는 부분을 지정하여 글자색 변경
        sp.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.cubox_sky))
                , 0
                , 6
                , Spannable.SPAN_INTERMEDIATE
        );
        // 해당내용 적용
        textView.setText(sp);
    }
    private void setImageView(){
        Glide.with(this)
                .load(SharedData.getIdBitmap())
                .placeholder(R.color.cubox_sky)
                .error(R.color.white)
                .into(idPreview);
    }
    private void setTint(ImageView view, @ColorRes int color){
        Drawable drawable = DrawableCompat.wrap(view.getDrawable());

        DrawableCompat.setTint(
                drawable.mutate(),
                ContextCompat.getColor(this, color)
        );
    }
}