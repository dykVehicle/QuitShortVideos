package com.screentime.guardian.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.screentime.guardian.MainActivity;
import com.screentime.guardian.R;
import com.screentime.guardian.util.Constants;
import com.screentime.guardian.util.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class UsageMonitorService extends Service {
    
    private static final String TAG = "UsageMonitorService";
    private static boolean isRunning = false;
    
    private Handler handler;
    private Runnable monitorRunnable;
    private UsageStatsManager usageStatsManager;
    private PreferenceManager preferenceManager;
    
    private long sessionStartTime = 0;
    private String currentForegroundApp = "";
    private long currentAppStartTime = 0;
    private long totalUsageTimeMs = 0;
    
    public static boolean isRunning() {
        return isRunning;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        handler = new Handler(Looper.getMainLooper());
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        preferenceManager = new PreferenceManager(this);
        
        createNotificationChannel();
        
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                checkUsage();
                handler.postDelayed(this, Constants.MONITORING_INTERVAL);
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        startForeground(Constants.NOTIFICATION_ID, createNotification());
        
        isRunning = true;
        sessionStartTime = System.currentTimeMillis();
        totalUsageTimeMs = 0;
        
        handler.post(monitorRunnable);
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        
        isRunning = false;
        handler.removeCallbacks(monitorRunnable);
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void checkUsage() {
        String foregroundApp = getForegroundApp();
        
        if (foregroundApp != null && isMonitoredApp(foregroundApp)) {
            if (!foregroundApp.equals(currentForegroundApp)) {
                // 切换到新的监控应用
                currentForegroundApp = foregroundApp;
                currentAppStartTime = System.currentTimeMillis();
                Log.d(TAG, "开始监控应用: " + foregroundApp);
            } else {
                // 继续使用同一个应用
                long now = System.currentTimeMillis();
                long usageDuration = now - currentAppStartTime;
                
                // 累计使用时间
                totalUsageTimeMs = usageDuration;
                
                int usageMinutes = (int) (totalUsageTimeMs / 60000);
                int timeLimit = preferenceManager.getTimeLimit();
                
                Log.d(TAG, "使用时间: " + usageMinutes + "/" + timeLimit + " 分钟");
                
                // 更新今日使用统计
                preferenceManager.setTodayUsageTime(usageMinutes);
                
                // 检查是否超时
                if (usageMinutes >= timeLimit) {
                    if (preferenceManager.canShowReminder()) {
                        showReminder(usageMinutes);
                    }
                }
            }
        } else {
            // 不是监控的应用，重置计时
            if (!currentForegroundApp.isEmpty()) {
                Log.d(TAG, "离开监控应用: " + currentForegroundApp);
                currentForegroundApp = "";
                // 重置使用时间（用户已经休息了）
                totalUsageTimeMs = 0;
                currentAppStartTime = 0;
            }
        }
    }
    
    private String getForegroundApp() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 10000; // 查询最近10秒
        
        UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        String foregroundApp = null;
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundApp = event.getPackageName();
            }
        }
        
        return foregroundApp;
    }
    
    private boolean isMonitoredApp(String packageName) {
        List<String> monitoredPackages = getMonitoredPackages();
        return monitoredPackages.contains(packageName);
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
    
    private void showReminder(int usageMinutes) {
        Log.d(TAG, "显示提醒，已使用: " + usageMinutes + " 分钟");
        
        preferenceManager.setLastReminderTime(System.currentTimeMillis());
        preferenceManager.incrementReminderCount();
        
        // 启动悬浮窗服务
        Intent overlayIntent = new Intent(this, OverlayService.class);
        overlayIntent.putExtra("usage_minutes", usageMinutes);
        startService(overlayIntent);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_description));
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_shield)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
