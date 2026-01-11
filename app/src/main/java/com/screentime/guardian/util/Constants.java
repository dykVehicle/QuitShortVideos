package com.screentime.guardian.util;

import java.util.Arrays;
import java.util.List;

public class Constants {
    
    // SharedPreferences
    public static final String PREF_NAME = "screen_time_guardian_prefs";
    public static final String PREF_TIME_LIMIT = "time_limit";
    public static final String PREF_MONITORING_ENABLED = "monitoring_enabled";
    public static final String PREF_MONITOR_DOUYIN = "monitor_douyin";
    public static final String PREF_MONITOR_KUAISHOU = "monitor_kuaishou";
    public static final String PREF_MONITOR_XIAOHONGSHU = "monitor_xiaohongshu";
    public static final String PREF_MONITOR_WECHAT = "monitor_wechat";
    public static final String PREF_TODAY_USAGE_TIME = "today_usage_time";
    public static final String PREF_TODAY_REMINDER_COUNT = "today_reminder_count";
    public static final String PREF_LAST_DATE = "last_date";
    public static final String PREF_LAST_REMINDER_TIME = "last_reminder_time";
    
    // 各平台使用时长偏好设置键
    public static final String PREF_USAGE_DOUYIN = "usage_douyin";
    public static final String PREF_USAGE_KUAISHOU = "usage_kuaishou";
    public static final String PREF_USAGE_XIAOHONGSHU = "usage_xiaohongshu";
    public static final String PREF_USAGE_WECHAT = "usage_wechat";
    
    // 广播动作
    public static final String ACTION_RESET_TIMER = "com.screentime.guardian.ACTION_RESET_TIMER";
    public static final String ACTION_REMINDER_DISMISSED = "com.screentime.guardian.ACTION_REMINDER_DISMISSED";
    
    // 默认时间限制（分钟）
    public static final int DEFAULT_TIME_LIMIT = 20;
    
    // 最大时间限制（分钟）- 10小时
    public static final int MAX_TIME_LIMIT = 600;
    
    // 提醒冷却时间（毫秒）- 1分钟内不重复提醒
    public static final long REMINDER_COOLDOWN = 60 * 1000;
    
    // 监控间隔（毫秒）
    public static final long MONITORING_INTERVAL = 3000; // 3秒，更及时地检测
    
    // 短视频应用包名
    public static final String PACKAGE_DOUYIN = "com.ss.android.ugc.aweme"; // 抖音
    public static final String PACKAGE_DOUYIN_LITE = "com.ss.android.ugc.aweme.lite"; // 抖音极速版
    public static final String PACKAGE_KUAISHOU = "com.smile.gifmaker"; // 快手
    public static final String PACKAGE_KUAISHOU_LITE = "com.kuaishou.nebula"; // 快手极速版
    public static final String PACKAGE_XIAOHONGSHU = "com.xingin.xhs"; // 小红书
    public static final String PACKAGE_WECHAT = "com.tencent.mm"; // 微信（包含视频号）
    public static final String PACKAGE_BILIBILI = "tv.danmaku.bili"; // B站
    
    // 所有监控的应用包名列表
    public static final List<String> DOUYIN_PACKAGES = Arrays.asList(PACKAGE_DOUYIN, PACKAGE_DOUYIN_LITE);
    public static final List<String> KUAISHOU_PACKAGES = Arrays.asList(PACKAGE_KUAISHOU, PACKAGE_KUAISHOU_LITE);
    public static final List<String> XIAOHONGSHU_PACKAGES = Arrays.asList(PACKAGE_XIAOHONGSHU);
    public static final List<String> WECHAT_PACKAGES = Arrays.asList(PACKAGE_WECHAT);
    
    // 通知
    public static final String NOTIFICATION_CHANNEL_ID = "screen_time_monitor";
    public static final int NOTIFICATION_ID = 1001;
    public static final int REMINDER_NOTIFICATION_ID = 1002;
}
