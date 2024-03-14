package aero.cubox.icheckerpassive;

import static android.os.Process.*;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class NoAccessActivity extends AppCompatActivity {
    private Button retryBtn;
    private Button exitBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_access);

        TextView errText = findViewById(R.id.err_msg);
        retryBtn = findViewById(R.id.retry_button);
        exitBtn = findViewById(R.id.exit_button);

        try{
            errText.setText(getIntent().getStringExtra("msg"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(NoAccessActivity.this, PermissionActivity.class));
                finish();
            }
        });

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveTaskToBack(true);						// 태스크를 백그라운드로 이동
                finishAndRemoveTask();						// 액티비티 종료 + 태스크 리스트에서 지우기
                killProcess(myPid());	// 앱 프로세스 종료
            }
        });
    }
}