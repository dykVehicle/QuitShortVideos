package com.example.digitaldetox;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class RestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏显示，覆盖状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_rest);

        Button btnClose = findViewById(R.id.btn_close_app);
        btnClose.setOnClickListener(v -> {
            // 回到桌面
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            
            // 结束当前 Activity
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // 屏蔽返回键，强制用户休息或点击关闭应用
        // super.onBackPressed(); 
    }
}
