package com.example.accessibilityplay;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

public class HeartbeatSender extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (CoreService.coreService == null) {
            return;
        }
        CoreService.coreService.syncAppMap();
        CoreService.coreService.checkDailyClean();
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "HEARTBEAT");
        CoreService.coreService.writeToFile("heartbeat_events", args);
    }
}
