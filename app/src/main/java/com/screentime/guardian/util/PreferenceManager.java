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
                .apply();
        }
    }
}
