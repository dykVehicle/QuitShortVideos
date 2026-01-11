package com.screentime.guardian.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.screentime.guardian.R;
import com.screentime.guardian.util.Constants;

public class OverlayService extends Service {
    
    private static final String TAG = "OverlayService";

    private WindowManager windowManager;
    private View overlayView;
    private int usageMinutes;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            usageMinutes = intent.getIntExtra("usage_minutes", 20);
        }
        
        if (Settings.canDrawOverlays(this)) {
            showOverlay();
        }
        
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlay();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showOverlay() {
        if (overlayView != null) {
            return; // 已经显示
        }

        // 震动提醒
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 200, 100, 200, 100, 200};
            vibrator.vibrate(pattern, -1);
        }

        // 创建悬浮窗
        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_reminder, null);

        // 设置消息
        TextView messageView = overlayView.findViewById(R.id.reminderMessage);
        String message = String.format(getString(R.string.time_exceeded_message), usageMinutes);
        messageView.setText(message);

        // 设置按钮点击事件
        Button btnTakeBreak = overlayView.findViewById(R.id.btnTakeBreak);
        Button btnContinue = overlayView.findViewById(R.id.btnContinue);

        btnTakeBreak.setOnClickListener(v -> {
            Log.d(TAG, "用户点击休息按钮");
            
            // 发送广播重置计时
            sendResetTimerBroadcast();
            
            // 返回桌面
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            
            removeOverlay();
            stopSelf();
        });

        btnContinue.setOnClickListener(v -> {
            Log.d(TAG, "用户点击继续使用按钮");
            
            // 发送广播重置计时
            sendResetTimerBroadcast();
            
            removeOverlay();
            stopSelf();
        });

        // 设置窗口参数
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        try {
            windowManager.addView(overlayView, params);
            
            // 允许按钮可点击
            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            windowManager.updateViewLayout(overlayView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            overlayView = null;
        }
    }
    
    /**
     * 发送广播通知 UsageMonitorService 重置计时
     */
    private void sendResetTimerBroadcast() {
        Log.d(TAG, "发送重置计时广播");
        Intent resetIntent = new Intent(Constants.ACTION_REMINDER_DISMISSED);
        resetIntent.setPackage(getPackageName());
        sendBroadcast(resetIntent);
    }
}
