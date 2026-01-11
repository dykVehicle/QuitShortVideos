package com.screentime.guardian.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.screentime.guardian.service.UsageMonitorService;
import com.screentime.guardian.util.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            PreferenceManager preferenceManager = new PreferenceManager(context);
            
            // 如果之前开启了监控，开机后自动启动服务
            if (preferenceManager.isMonitoringEnabled()) {
                Intent serviceIntent = new Intent(context, UsageMonitorService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}
