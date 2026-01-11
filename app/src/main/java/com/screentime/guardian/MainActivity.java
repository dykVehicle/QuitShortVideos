package com.screentime.guardian;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.screentime.guardian.service.UsageMonitorService;
import com.screentime.guardian.util.Constants;
import com.screentime.guardian.util.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private MaterialSwitch monitoringSwitch;
    private View statusIndicator;
    private TextView statusText;
    private Slider timeLimitSlider;
    private TextView timeLimitValue;
    private TextView totalTimeValue;
    private TextView reminderCountValue;
    
    private MaterialSwitch switchDouyin;
    private MaterialSwitch switchKuaishou;
    private MaterialSwitch switchXiaohongshu;
    private MaterialSwitch switchWechat;
    
    private ImageView usagePermissionStatus;
    private ImageView overlayPermissionStatus;
    
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        preferenceManager = new PreferenceManager(this);
        
        initViews();
        setupListeners();
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        updateServiceStatus();
        updateStats();
    }

    private void initViews() {
        monitoringSwitch = findViewById(R.id.monitoringSwitch);
        statusIndicator = findViewById(R.id.statusIndicator);
        statusText = findViewById(R.id.statusText);
        timeLimitSlider = findViewById(R.id.timeLimitSlider);
        timeLimitValue = findViewById(R.id.timeLimitValue);
        totalTimeValue = findViewById(R.id.totalTimeValue);
        reminderCountValue = findViewById(R.id.reminderCountValue);
        
        switchDouyin = findViewById(R.id.switchDouyin);
        switchKuaishou = findViewById(R.id.switchKuaishou);
        switchXiaohongshu = findViewById(R.id.switchXiaohongshu);
        switchWechat = findViewById(R.id.switchWechat);
        
        usagePermissionStatus = findViewById(R.id.usagePermissionStatus);
        overlayPermissionStatus = findViewById(R.id.overlayPermissionStatus);
        
        findViewById(R.id.usagePermissionLayout).setOnClickListener(v -> requestUsageStatsPermission());
        findViewById(R.id.overlayPermissionLayout).setOnClickListener(v -> requestOverlayPermission());
    }

    private void setupListeners() {
        monitoringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!hasRequiredPermissions()) {
                    monitoringSwitch.setChecked(false);
                    Toast.makeText(this, "请先授予必要权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                startMonitoringService();
            } else {
                stopMonitoringService();
            }
            updateServiceStatus();
        });

        timeLimitSlider.addOnChangeListener((slider, value, fromUser) -> {
            int minutes = (int) value;
            timeLimitValue.setText(minutes + "分钟");
            if (fromUser) {
                preferenceManager.setTimeLimit(minutes);
            }
        });

        setupAppSwitchListener(switchDouyin, Constants.PREF_MONITOR_DOUYIN);
        setupAppSwitchListener(switchKuaishou, Constants.PREF_MONITOR_KUAISHOU);
        setupAppSwitchListener(switchXiaohongshu, Constants.PREF_MONITOR_XIAOHONGSHU);
        setupAppSwitchListener(switchWechat, Constants.PREF_MONITOR_WECHAT);
    }

    private void setupAppSwitchListener(MaterialSwitch switchView, String prefKey) {
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setAppMonitored(prefKey, isChecked);
        });
    }

    private void loadSettings() {
        int timeLimit = preferenceManager.getTimeLimit();
        timeLimitSlider.setValue(timeLimit);
        timeLimitValue.setText(timeLimit + "分钟");

        switchDouyin.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_DOUYIN));
        switchKuaishou.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_KUAISHOU));
        switchXiaohongshu.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_XIAOHONGSHU));
        switchWechat.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_WECHAT));
    }

    private void updateServiceStatus() {
        boolean isRunning = UsageMonitorService.isRunning();
        monitoringSwitch.setChecked(isRunning);
        
        if (isRunning) {
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
            statusText.setText(R.string.monitoring_active);
        } else {
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_inactive);
            statusText.setText(R.string.monitoring_inactive);
        }
    }

    private void updateStats() {
        int totalMinutes = preferenceManager.getTodayUsageTime();
        int reminderCount = preferenceManager.getTodayReminderCount();
        
        totalTimeValue.setText(String.valueOf(totalMinutes));
        reminderCountValue.setText(String.valueOf(reminderCount));
    }

    private void updatePermissionStatus() {
        boolean hasUsagePermission = hasUsageStatsPermission();
        boolean hasOverlayPermission = hasOverlayPermission();
        
        usagePermissionStatus.setImageResource(hasUsagePermission ? R.drawable.ic_check : R.drawable.ic_warning);
        usagePermissionStatus.setColorFilter(getColor(hasUsagePermission ? R.color.success : R.color.warning));
        
        overlayPermissionStatus.setImageResource(hasOverlayPermission ? R.drawable.ic_check : R.drawable.ic_warning);
        overlayPermissionStatus.setColorFilter(getColor(hasOverlayPermission ? R.color.success : R.color.warning));
    }

    private boolean hasRequiredPermissions() {
        return hasUsageStatsPermission() && hasOverlayPermission();
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private void requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "请找到并启用【屏幕卫士】", Toast.LENGTH_LONG).show();
        }
    }

    private void requestOverlayPermission() {
        if (!hasOverlayPermission()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, UsageMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        preferenceManager.setMonitoringEnabled(true);
    }

    private void stopMonitoringService() {
        Intent serviceIntent = new Intent(this, UsageMonitorService.class);
        stopService(serviceIntent);
        preferenceManager.setMonitoringEnabled(false);
    }
}
