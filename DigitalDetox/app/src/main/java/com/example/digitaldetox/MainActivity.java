package com.example.digitaldetox;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnOverlay;
    private Button btnAccessibility;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOverlay = findViewById(R.id.btn_overlay);
        btnAccessibility = findViewById(R.id.btn_accessibility);
        tvStatus = findViewById(R.id.tv_status);

        btnOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "悬浮窗权限已开启", Toast.LENGTH_SHORT).show();
            }
        });

        btnAccessibility.setOnClickListener(v -> {
            if (!isAccessibilityServiceEnabled()) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            } else {
                Toast.makeText(this, "无障碍服务已开启", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean accessibilityGranted = isAccessibilityServiceEnabled();

        btnOverlay.setEnabled(!overlayGranted);
        btnOverlay.setText(overlayGranted ? "悬浮窗权限：已开启" : "开启悬浮窗权限");

        btnAccessibility.setEnabled(!accessibilityGranted);
        btnAccessibility.setText(accessibilityGranted ? "无障碍服务：已开启" : "开启无障碍服务");

        if (overlayGranted && accessibilityGranted) {
            tvStatus.setText("监控运行中... 请保持后台运行");
        } else {
            tvStatus.setText("请开启上述所有权限以开始监控");
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
