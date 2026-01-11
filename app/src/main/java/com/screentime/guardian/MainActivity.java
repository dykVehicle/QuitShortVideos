package com.screentime.guardian;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

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
    
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        preferenceManager = new PreferenceManager(this);
        refreshHandler = new Handler(Looper.getMainLooper());
        
        initViews();
        setupListeners();
        loadSettings();
        
        // 定时刷新统计数据
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                updateStats();
                refreshHandler.postDelayed(this, 5000); // 每5秒刷新
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        updateServiceStatus();
        updateStats();
        
        // 开始定时刷新
        refreshHandler.post(refreshRunnable);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 停止定时刷新
        refreshHandler.removeCallbacks(refreshRunnable);
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
            timeLimitValue.setText(formatTime(minutes));
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
        timeLimitValue.setText(formatTime(timeLimit));

        switchDouyin.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_DOUYIN));
        switchKuaishou.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_KUAISHOU));
        switchXiaohongshu.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_XIAOHONGSHU));
        switchWechat.setChecked(preferenceManager.isAppMonitored(Constants.PREF_MONITOR_WECHAT));
    }
    
    /**
     * 格式化时间显示
     * @param minutes 分钟数
     * @return 格式化的时间字符串
     */
    private String formatTime(int minutes) {
        if (minutes == 0) {
            return "关闭提醒";
        } else if (minutes < 60) {
            return minutes + "分钟";
        } else {
            int hours = minutes / 60;
            int mins = minutes % 60;
            if (mins == 0) {
                return hours + "小时";
            } else {
                return hours + "小时" + mins + "分钟";
            }
        }
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
        int refreshedMinutes = getTodayUsageTimeFromSystem();
        if (refreshedMinutes >= 0) {
            totalMinutes = refreshedMinutes;
            preferenceManager.setTodayUsageTime(totalMinutes);
        }
        int reminderCount = preferenceManager.getTodayReminderCount();
        
        totalTimeValue.setText(String.valueOf(totalMinutes));
        reminderCountValue.setText(String.valueOf(reminderCount));
    }

    /**
     * 从系统 UsageStats 刷新今日短视频使用时长（分钟）。
     * @return 分钟数；如果无权限或获取失败返回 -1
     */
    private int getTodayUsageTimeFromSystem() {
        if (!hasUsageStatsPermission()) {
            return -1;
        }

        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usageStatsManager == null) {
                return -1;
            }

            // 获取今天的开始时间
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long todayStart = calendar.getTimeInMillis();
            long now = System.currentTimeMillis();

            Map<String, UsageStats> statsMap = usageStatsManager.queryAndAggregateUsageStats(todayStart, now);
            if (statsMap == null) {
                return 0;
            }

            long totalUsageMs = 0;
            for (String packageName : getMonitoredPackages()) {
                UsageStats stats = statsMap.get(packageName);
                if (stats != null) {
                    totalUsageMs += stats.getTotalTimeInForeground();
                }
            }

            return (int) (totalUsageMs / 60000);
        } catch (Exception e) {
            return -1;
        }
    }

    private List<String> getMonitoredPackages() {
        List<String> packages = new ArrayList<>();

        if (preferenceManager.isAppMonitored(Constants.PREF_MONITOR_DOUYIN)) {
            packages.addAll(Constants.DOUYIN_PACKAGES);
        }
        if (preferenceManager.isAppMonitored(Constants.PREF_MONITOR_KUAISHOU)) {
            packages.addAll(Constants.KUAISHOU_PACKAGES);
        }
        if (preferenceManager.isAppMonitored(Constants.PREF_MONITOR_XIAOHONGSHU)) {
            packages.addAll(Constants.XIAOHONGSHU_PACKAGES);
        }
        if (preferenceManager.isAppMonitored(Constants.PREF_MONITOR_WECHAT)) {
            packages.addAll(Constants.WECHAT_PACKAGES);
        }

        return packages;
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
