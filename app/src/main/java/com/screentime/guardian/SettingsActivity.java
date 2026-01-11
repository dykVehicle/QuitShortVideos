package com.screentime.guardian;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 预留设置页（当前版本未提供完整设置界面）。
 * Manifest 中已注册该 Activity，保留一个最小实现以通过构建/检查。
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText("设置页开发中");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        tv.setPadding(padding, padding, padding, padding);
        setContentView(tv);
    }
}
