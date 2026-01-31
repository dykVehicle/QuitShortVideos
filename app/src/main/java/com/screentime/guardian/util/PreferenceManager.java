package com.screentime.guardian.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PreferenceManager {
    
    private final SharedPreferences prefs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public int getTimeLimit() {
        return prefs.getInt(Constants.PREF_TIME_LIMIT, Constants.DEFAULT_TIME_LIMIT);
    }
    
    public void setTimeLimit(int minutes) {
        prefs.edit().putInt(Constants.PREF_TIME_LIMIT, minutes).apply();
    }
    
    public boolean isMonitoringEnabled() {
        return prefs.getBoolean(Constants.PREF_MONITORING_ENABLED, false);
    }
    
    public void setMonitoringEnabled(boolean enabled) {
        prefs.edit().putBoolean(Constants.PREF_MONITORING_ENABLED, enabled).apply();
    }
    
    public boolean isAppMonitored(String prefKey) {
        return prefs.getBoolean(prefKey, true);
    }
    
    public void setAppMonitored(String prefKey, boolean monitored) {
        prefs.edit().putBoolean(prefKey, monitored).apply();
    }
    
    public int getTodayUsageTime() {
        checkAndResetDate();
        return prefs.getInt(Constants.PREF_TODAY_USAGE_TIME, 0);
    }
    
    public void setTodayUsageTime(int minutes) {
        checkAndResetDate();
        prefs.edit().putInt(Constants.PREF_TODAY_USAGE_TIME, minutes).apply();
    }
    
    public void addUsageTime(int minutes) {
        int current = getTodayUsageTime();
        setTodayUsageTime(current + minutes);
    }
    
    public int getTodayReminderCount() {
        checkAndResetDate();
        return prefs.getInt(Constants.PREF_TODAY_REMINDER_COUNT, 0);
    }
    
    public void incrementReminderCount() {
        checkAndResetDate();
        int count = getTodayReminderCount();
        prefs.edit().putInt(Constants.PREF_TODAY_REMINDER_COUNT, count + 1).apply();
    }
    
    public long getLastReminderTime() {
        return prefs.getLong(Constants.PREF_LAST_REMINDER_TIME, 0);
    }
    
    public void setLastReminderTime(long time) {
        prefs.edit().putLong(Constants.PREF_LAST_REMINDER_TIME, time).apply();
    }
    
    public boolean canShowReminder() {
        long lastReminder = getLastReminderTime();
        long now = System.currentTimeMillis();
        return (now - lastReminder) >= Constants.REMINDER_COOLDOWN;
    }
    
    private void checkAndResetDate() {
        String today = dateFormat.format(new Date());
        String lastDate = prefs.getString(Constants.PREF_LAST_DATE, "");
        
        if (!today.equals(lastDate)) {
            // 新的一天，重置统计数据
            prefs.edit()
                .putString(Constants.PREF_LAST_DATE, today)
                .putInt(Constants.PREF_TODAY_USAGE_TIME, 0)
                .putInt(Constants.PREF_TODAY_REMINDER_COUNT, 0)
                .putInt(Constants.PREF_USAGE_DOUYIN, 0)
                .putInt(Constants.PREF_USAGE_KUAISHOU, 0)
                .putInt(Constants.PREF_USAGE_XIAOHONGSHU, 0)
                .putInt(Constants.PREF_USAGE_WECHAT, 0)
                .apply();
        }
    }
    
    /**
     * 获取抖音今日使用时长（分钟）
     */
    public int getDouyinUsageTime() {
        checkAndResetDate();
        return prefs.getInt(Constants.PREF_USAGE_DOUYIN, 0);
    }
    
    /**
     * 设置抖音今日使用时长（分钟）
     */
    public void setDouyinUsageTime(int minutes) {
        checkAndResetDate();
        prefs.edit().putInt(Constants.PREF_USAGE_DOUYIN, minutes).apply();
    }
    
    /**
     * 获取快手今日使用时长（分钟）
     */
    public int getKuaishouUsageTime() {
        checkAndResetDate();
        return prefs.getInt(Constants.PREF_USAGE_KUAISHOU, 0);
    }
    
    /**
     * 设置快手今日使用时长（分钟）
     */
    public void setKuaishouUsageTime(int minutes) {
        checkAndResetDate();
        prefs.edit().putInt(Constants.PREF_USAGE_KUAISHOU, minutes).apply();
    }
    
    /**
     * 获取小红书今日使用时长（分钟）
     */
    public int getXiaohongshuUsageTime() {
        checkAndResetDate();
        return prefs.getInt(Constants.PREF_USAGE_XIAOHONGSHU, 0);
    }
    
    /**
     * 设置小红书今日使用时长（分钟）
     */
    public void setXiaohongshuUsageTime(int minutes) {
        checkAndResetDate();
        prefs.edit().putInt(Constants.PREF_USAGE_XIAOHONGSHU, minutes).apply();
    }
    
    /**
     * 获取微信今日使用时长（分钟）
     */
    public int getWechatUsageTime() {
        checkAndResetDate();
        return prefs.getInt(Constants.PREF_USAGE_WECHAT, 0);
    }
    
    /**
     * 设置微信今日使用时长（分钟）
     */
    public void setWechatUsageTime(int minutes) {
        checkAndResetDate();
        prefs.edit().putInt(Constants.PREF_USAGE_WECHAT, minutes).apply();
    }
    
    /**
     * 重置今日所有统计数据
     */
    public void resetTodayStats() {
        prefs.edit()
            .putInt(Constants.PREF_TODAY_USAGE_TIME, 0)
            .putInt(Constants.PREF_TODAY_REMINDER_COUNT, 0)
            .putInt(Constants.PREF_USAGE_DOUYIN, 0)
            .putInt(Constants.PREF_USAGE_KUAISHOU, 0)
            .putInt(Constants.PREF_USAGE_XIAOHONGSHU, 0)
            .putInt(Constants.PREF_USAGE_WECHAT, 0)
            .apply();
    }
}
