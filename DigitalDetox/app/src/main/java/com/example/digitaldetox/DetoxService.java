package com.example.digitaldetox;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DetoxService extends AccessibilityService {

    private static final String TAG = "DetoxService";
    // 20分钟 = 20 * 60 * 1000 毫秒
    private static final long MAX_USAGE_TIME_MS = 20 * 60 * 1000;
    
    // 目标应用包名列表
    private static final Set<String> TARGET_PACKAGES = new HashSet<>(Arrays.asList(
            "com.ss.android.ugc.aweme", // 抖音
            "com.smile.gifmaker",       // 快手
            "com.xingin.xhs"            // 小红书
    ));

    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    
    // 记录开始刷视频的时间
    private long startTime = 0;
    // 当前是否在监控目标应用中
    private boolean isWatching = false;
    private String currentPackage = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        // Log.d(TAG, "Event: pkg=" + packageName + " class=" + className);

        boolean isTarget = false;

        // 1. 检查直接匹配的短视频应用
        if (TARGET_PACKAGES.contains(packageName)) {
            isTarget = true;
        } 
        // 2. 检查微信视频号 (简单的模糊匹配)
        // 注意：微信类名混淆严重，这里尝试匹配常见的视频号组件关键词 "Finder" 或 "Video"
        else if (WECHAT_PACKAGE.equals(packageName)) {
            if (className.contains("Finder") || className.contains("Video") || className.contains("plugin.finder")) {
                isTarget = true;
            } else {
                // 如果在微信但不是视频号，视为离开
                isTarget = false;
            }
        }

        updateUsageState(isTarget);
    }

    private void updateUsageState(boolean isTarget) {
        long now = System.currentTimeMillis();

        if (isTarget) {
            if (!isWatching) {
                // 刚进入目标应用
                isWatching = true;
                startTime = now;
                Log.i(TAG, "开始监控短视频使用...");
            } else {
                // 正在观看中，检查时长
                long duration = now - startTime;
                if (duration > MAX_USAGE_TIME_MS) {
                    Log.w(TAG, "使用超时！时长: " + duration + "ms");
                    triggerRest();
                    // 重置开始时间，避免无限弹出，或者选择强制阻塞直到用户手动关闭
                    // 这里为了体验，弹出后重置，如果用户继续看，会在下一次事件中重新计时
                    // 但由于RestActivity是全屏覆盖，用户不点关闭是回不去的
                    startTime = now; 
                }
            }
        } else {
            // 不在目标应用中
            if (isWatching) {
                Log.i(TAG, "停止监控，用户离开了短视频应用");
                isWatching = false;
                startTime = 0;
            }
        }
    }

    private void triggerRest() {
        Intent intent = new Intent(this, RestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {
        isWatching = false;
        startTime = 0;
    }
}
