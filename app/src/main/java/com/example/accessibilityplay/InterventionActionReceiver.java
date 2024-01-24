package com.example.accessibilityplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.ArrayMap;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;

public class InterventionActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long time = intent.getLongExtra("extra time", 0);
        String pack = CoreService.currentTargetPackage;
        Log.d("Receiver", "onReceive: " + intent.getLongExtra("extra time", 0) + " " + pack + " " + CoreService.currentTargetPackage);
        CoreService.packageInExtraTime.put(pack, true);
        CoreService.coreService.closeOverlayWindow();
        CoreService.coreService.retrievePackageOneMinuteClicked();
        if ((!CoreService.packageOneMinuteClicked.containsKey(pack) || !CoreService.packageOneMinuteClicked.get(pack)) && time == 60000L) {
            CoreService.packageOneMinuteClicked.put(pack, true);
            CoreService.coreService.storePackageOneMinuteClicked();
        }
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(CoreService.coreService);
        notificationManagerCompat.cancel(2);
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "MORE_USAGE");
        args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
        args.put("package_name", CoreService.currentTargetPackage);
        args.put("extra_time_in_ms", time);
        CoreService.coreService.writeToFile("user_actions", args);
        if (time > 15 * 60000) {
            return;
        }
        CoreService.packageCountDownTimer.put(pack, new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long l) {
                Log.d("TAG", "onTicking: " + pack + ' ' + l / 1000 + "s " + CoreService.packageInExtraTime.get(pack));
            }

            @Override
            public void onFinish() {
                CoreService.packageInExtraTime.put(pack, false);
                if (CoreService.currentForegroundPackage.equals(CoreService.currentTargetPackage)) {
                    CoreService.coreService.launchInterventionWithCode(CoreService.currentTargetPackage);
                }
            }
        }.start());

    }
}
