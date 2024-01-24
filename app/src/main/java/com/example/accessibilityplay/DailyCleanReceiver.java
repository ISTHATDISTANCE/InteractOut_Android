package com.example.accessibilityplay;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class DailyCleanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            CoreService.coreService.dailyClean(false);
            Intent dailyCleanIntent = new Intent(CoreService.coreService, DailyCleanReceiver.class);
            PendingIntent dailyCleanPendingIntent = PendingIntent.getBroadcast(CoreService.coreService, 11, dailyCleanIntent, PendingIntent.FLAG_IMMUTABLE);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 4);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Calendar now = Calendar.getInstance();
            if (now.after(calendar)) {
                calendar.add(Calendar.DATE, 1);
            }
            AlarmManager alarmManager = (AlarmManager) CoreService.coreService.getSystemService(ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyCleanPendingIntent);
        } catch (NullPointerException ignored) {
            CoreService.isDailyCleanFail = true;
        }
    }
}
