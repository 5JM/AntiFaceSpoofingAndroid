package aero.cubox.icheckerpassive;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.Vector;

import aero.cubox.communication.SharedData;

public class ResultActivity extends AppCompatActivity {
    private ImageView idView;
    private ImageView liveView;
    private TextView thr1Result;
    private TextView thr1;
    private TextView thr2Result;
    private TextView thr2;
    private TextView thr3Result;
    private TextView thr3;
    private TextView thr4Result;
    private TextView thr4;
    private TextView resultText;
    private TextView resultContext;
    private Button returnButton;
    private Button reLivenessButton;
    private Vector<Float> thrsR;
    private Vector<Float> thrs;
    private LinearLayout thrsContainer;
    private TextView guideText;
    private Boolean showThrs = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // loads thresholds
        thrsR = SharedData.thrsArr; // live thresholds
        thrs = SharedData.thrs; // thresholds

        initComponents();

        setImageView(SharedData.getIdBitmap(),idView );
        setImageView(SharedData.first_live_img,liveView );

        setMsg();

        guideText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(showThrs) viewThrs();
                else goneThrs();
                showThrs = !showThrs;
            }
        });

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetImg();
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        reLivenessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedData.first_live_img = null;
                Intent intent = new Intent(ResultActivity.this, EstimateActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setImageView(Bitmap input, ImageView _imageView){
        Glide.with(this)
                .load(input)
                .placeholder(R.color.cubox_sky)
                .error(R.color.white)
                .into(_imageView);
    }

    private void initComponents(){
        guideText = findViewById(R.id.guide_text);
        idView = findViewById(R.id.id_view);
        liveView = findViewById(R.id.live_view);

        thr1Result = findViewById(R.id.thr1_result);
        thr1 = findViewById(R.id.thr1);
        thr2Result = findViewById(R.id.thr2_result);
        thr2 = findViewById(R.id.thr2);
        thr3Result = findViewById(R.id.thr3_result);
        thr3 = findViewById(R.id.thr3);
        thr4Result = findViewById(R.id.thr4_result);
        thr4 = findViewById(R.id.thr4);

        thrsContainer = findViewById(R.id.thrs_container);

        thr1Result.setText(thrsR.get(0).toString());
        thr2Result.setText(thrsR.get(1).toString());
        thr3Result.setText(thrsR.get(2).toString());
        thr4Result.setText(thrsR.get(3).toString());

        //todo : 서버 임계치를 뿌리는거
        thr1.setText(thrs.get(0).toString());
        thr2.setText(thrs.get(1).toString());
        thr3.setText(thrs.get(2).toString());
        thr4.setText(thrs.get(3).toString());

        //todo : 내부 코어 임계치를 뿌리는거 ( fps : 20미만일때, 서버 임계치와 달라질 수 있음 )
//        thr1.setText(thrsR.get(4).toString());
//        thr2.setText(thrsR.get(5).toString());
//        thr3.setText(thrsR.get(6).toString());
//        thr4.setText(thrsR.get(7).toString());

        resultText = findViewById(R.id.result_text);
        resultContext = findViewById(R.id.result_context);

        returnButton = findViewById(R.id.return_button);
        reLivenessButton = findViewById(R.id.re_liveness_button);

        goneThrs();
    }

    private void setMsg(){
        try{
            resultText.setText(getIntent().getStringExtra("guide"));
            resultContext.setText(getIntent().getStringExtra("msg"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void resetImg(){
        SharedData.in_img = null;
        SharedData.first_live_img = null;
    }

    private void goneThrs(){
        thrsContainer.setVisibility(View.INVISIBLE);
    }
    private void viewThrs(){
        thrsContainer.setVisibility(View.VISIBLE);
    }
}