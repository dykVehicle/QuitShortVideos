package com.screentime.guardian.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
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
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class UsageMonitorService extends Service {
    
    private static final String TAG = "UsageMonitorService";
    private static boolean isRunning = false;
    
    private Handler handler;
    private Runnable monitorRunnable;
    private UsageStatsManager usageStatsManager;
    private PreferenceManager preferenceManager;
    
    // 当前会话的连续使用时间跟踪
    private String currentForegroundApp = "";
    private long continuousUsageStartTime = 0;  // 连续使用开始时间
    private long accumulatedUsageMs = 0;        // 累计使用时间（毫秒）
    private long lastCheckTime = 0;             // 上次检查时间
    
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
        
        // 重置连续使用计时
        resetContinuousUsage();
        lastCheckTime = System.currentTimeMillis();
        
        handler.post(monitorRunnable);
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        
        // 服务退出前刷新一次统计，避免最后一段使用时间未落盘
        updateTodayStats();

        isRunning = false;
        handler.removeCallbacks(monitorRunnable);
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void resetContinuousUsage() {
        currentForegroundApp = "";
        continuousUsageStartTime = 0;
        accumulatedUsageMs = 0;
    }
    
    private void checkUsage() {
        long currentTime = System.currentTimeMillis();
        String foregroundApp = getForegroundApp();
        
        Log.d(TAG, "检测前台应用: " + (foregroundApp != null ? foregroundApp : "null"));
        
        if (foregroundApp != null && isMonitoredApp(foregroundApp)) {
            // 正在使用监控的短视频应用
            if (currentForegroundApp.isEmpty() || !isMonitoredApp(currentForegroundApp)) {
                // 刚开始使用短视频应用
                continuousUsageStartTime = currentTime;
                currentForegroundApp = foregroundApp;
                Log.d(TAG, "开始监控短视频应用: " + foregroundApp);
            } else {
                // 继续使用短视频应用（可能切换了不同的短视频应用，但都算连续使用）
                currentForegroundApp = foregroundApp;
            }
            
            // 计算从开始到现在的连续使用时间
            if (continuousUsageStartTime > 0) {
                accumulatedUsageMs = currentTime - continuousUsageStartTime;
            }
            
            int usageMinutes = (int) (accumulatedUsageMs / 60000);
            int timeLimit = preferenceManager.getTimeLimit();
            
            Log.d(TAG, "连续使用时间: " + usageMinutes + "/" + timeLimit + " 分钟");
            
            // 更新今日使用统计（从UsageStats获取更准确的数据）
            updateTodayStats();
            
            // 检查是否超时
            if (usageMinutes >= timeLimit && timeLimit > 0) {
                if (preferenceManager.canShowReminder()) {
                    showReminder(usageMinutes);
                }
            }
        } else {
            // 不是监控的应用
            if (!currentForegroundApp.isEmpty() && isMonitoredApp(currentForegroundApp)) {
                Log.d(TAG, "离开短视频应用: " + currentForegroundApp);
                // 很多系统会在应用“离开前台”时才结算 UsageStats 的前台时长，
                // 这里只在使用中更新会导致用户看完退出后仍显示 0 分钟。
                updateTodayStats();
                // 用户离开了短视频应用，重置连续使用计时
                resetContinuousUsage();
            }
            currentForegroundApp = foregroundApp != null ? foregroundApp : "";
        }
        
        lastCheckTime = currentTime;
    }
    
    /**
     * 获取当前前台应用
     */
    private String getForegroundApp() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 60000; // 查询最近1分钟
        
        // 方法1: 使用 UsageEvents 获取最近的前台应用
        try {
            UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();
            String foregroundApp = null;
            long latestTime = 0;
            
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if (event.getTimeStamp() > latestTime) {
                        latestTime = event.getTimeStamp();
                        foregroundApp = event.getPackageName();
                    }
                }
            }
            
            if (foregroundApp != null) {
                return foregroundApp;
            }
        } catch (Exception e) {
            Log.e(TAG, "queryEvents 失败: " + e.getMessage());
        }
        
        // 方法2: 使用 UsageStats 作为备选
        try {
            Map<String, UsageStats> statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
            if (statsMap != null && !statsMap.isEmpty()) {
                String topPackage = null;
                long topTime = 0;
                
                for (UsageStats stats : statsMap.values()) {
                    if (stats.getLastTimeUsed() > topTime) {
                        topTime = stats.getLastTimeUsed();
                        topPackage = stats.getPackageName();
                    }
                }
                return topPackage;
            }
        } catch (Exception e) {
            Log.e(TAG, "queryAndAggregateUsageStats 失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 更新今日使用统计
     */
    private void updateTodayStats() {
        try {
            // 获取今天的开始时间
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long todayStart = calendar.getTimeInMillis();
            long now = System.currentTimeMillis();
            
            // 查询今日使用统计
            Map<String, UsageStats> statsMap = usageStatsManager.queryAndAggregateUsageStats(todayStart, now);
            
            if (statsMap != null) {
                long totalUsageMs = 0;
                List<String> monitoredPackages = getMonitoredPackages();
                
                for (String packageName : monitoredPackages) {
                    UsageStats stats = statsMap.get(packageName);
                    if (stats != null) {
                        totalUsageMs += stats.getTotalTimeInForeground();
                        Log.d(TAG, packageName + " 今日使用: " + (stats.getTotalTimeInForeground() / 60000) + " 分钟");
                    }
                }
                
                int totalMinutes = (int) (totalUsageMs / 60000);
                preferenceManager.setTodayUsageTime(totalMinutes);
                Log.d(TAG, "今日总使用时间: " + totalMinutes + " 分钟");
            }
        } catch (Exception e) {
            Log.e(TAG, "更新今日统计失败: " + e.getMessage());
        }
    }
    
    private boolean isMonitoredApp(String packageName) {
        if (packageName == null) return false;
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
        Log.d(TAG, "显示提醒，已连续使用: " + usageMinutes + " 分钟");
        
        preferenceManager.setLastReminderTime(System.currentTimeMillis());
        preferenceManager.incrementReminderCount();
        
        // 重置连续使用计时（提醒后重新计时）
        continuousUsageStartTime = System.currentTimeMillis();
        accumulatedUsageMs = 0;
        
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
