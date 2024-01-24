package com.example.accessibilityplay;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public class CoreService extends AccessibilityService {
    public static CoreService coreService;
    public Window window;
    public static boolean reverseDirection = false,
            isInLabMode = false,
            isDisablingWindowAllowed = false,
            isLongpressListenerOn = false,
            isOverlayOn = false,
            inTutorialMainPage = true,
            isInTutorial = false,
            isDoubleTapToSingleTap = false,
            usingDefaultIntervention = true,
            isDefaultInterventionLaunched = false,
            customizeIntervention = false,
            isScreenOn = true,
            isTapAssistance = false,
            isInterventionOn = false,
            isDailyCleanFail = false;
    public static long targetTimeFieldStudy = 0; //3600000L; // 1 hour free use, but currently 1 min for test
    public static int tapDelay = 0, tapDelayMax = 0;
    public static int swipeDelay = 0, swipeDelayMax = 0;
    public static int minimumFingerToTap = 1;
    // 50 to 100 ms time is good for prolong
    public static int swipeFingers = 1;
    public static int xOffset = 0;
    public static int yOffset = 0;
    public static int prolongMax = 0;
    public static double swipeRatio = 1, scrollRatioMax = 1, swipeRatioExp = 0;
    public static int prolongNoteShowTime = 100;
    public static int screenWidth = 1080, screenHeight = 2400, tapAssistanceOffset;
    public static long currentForegroundEnterTime = 0;
    public static String currentForegroundPackage = "", currentTargetPackage = "";
    public static String participantFilename;
    public static String participantDisplayName;
    public static String swipeInt, tapInt;
    public static Vector<String> appChosen = new Vector<>(), packageChosen = new Vector<>(), systemPackages = new Vector<>();
    public static ArrayMap<String, Long> packageUsedTime = new ArrayMap<>(), appTargetTime = new ArrayMap<>(), packageGrantedTime = new ArrayMap<>();
    public static ArrayMap<String, Integer> tapDelayValues = new ArrayMap<>(), swipeDelayValues = new ArrayMap<>(), tapProlongValues = new ArrayMap<>();
    public static ArrayMap<String, Double> swipeRatioExpValues = new ArrayMap<>();
    public static ArrayMap<String, String> packageNameMap = new ArrayMap<>(), packageInterventionCode = new ArrayMap<>();
    public static ArrayMap<String, Boolean> packageInExtraTime, packageOneMinuteClicked = new ArrayMap<>();
    public static ArrayMap<String, CountDownTimer> packageCountDownTimer = new ArrayMap<>();
    public FirebaseFirestore db;

    private final static String TAG = "MyAccessibilityService.java";
    private CountDownTimer countDownTimer;
    private boolean isCountdownLaunched = false;
    private final long SATURATION_NUM = 1; // after 10 interactions, call increaseIntensity()
    private int currentStep = 0;
    private Handler interventionUpdateHandler;
    private PendingIntent checkTimeIntent, dailyCleanPendingIntent;
    private Runnable interventionUpdateRunnable;
    private SharedPreferences sharedPreferences;
    private Semaphore semaphore = new Semaphore(1);
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceiveIntent: " + intent.getCategories() + " " + action);
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                isScreenOn = false;
                closeOverlayWindow();
                updateUseTime(currentForegroundPackage, "OFF");
                currentForegroundPackage = "OFF";
                if (isCountdownLaunched) {
                    countDownTimer.cancel();
                    isCountdownLaunched = false;
                }
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(coreService);
                notificationManagerCompat.cancel(2);
                ArrayMap<String, Object> args = new ArrayMap<>();
                args.put("info", "SCREEN_OFF");
                CoreService.coreService.writeToFile("state_changes", args);

            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                isScreenOn = true;
                ArrayMap<String, Object> args = new ArrayMap<>();
                retrievePackageUsedTime();
                if (packageUsedTime.containsKey(currentForegroundPackage)) {
                    doSthWhenEnteringTarget();
                }
                args.put("info", "SCREEN_ON");
                CoreService.coreService.writeToFile("state_changes", args);
            }
            else if (intent.getAction().equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                // this intent action is deprecated at API level 31, when this code is written. But it is working.
                ArrayMap<String, Object> args = new ArrayMap<>();
                args.put("info", "ENTER_HOME");
                CoreService.coreService.writeToFile("state_changes", args);
                updateUseTime(currentForegroundPackage, "CLOSE_SYSTEM_DIALOGS");
                currentForegroundPackage = "CLOSE_SYSTEM_DIALOGS";
                closeOverlayWindow();
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(coreService);
                notificationManagerCompat.cancel(2);
                if (isCountdownLaunched) {
                    countDownTimer.cancel();
                    isCountdownLaunched = false;
                }
            }
        }
    };

    public void checkDailyClean() {
        if (isDailyCleanFail) {
            dailyClean(false);
            Intent dailyCleanIntent = new Intent(this, DailyCleanReceiver.class);
            PendingIntent dailyCleanPendingIntent = PendingIntent.getBroadcast(this, 11, dailyCleanIntent, PendingIntent.FLAG_IMMUTABLE);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 4);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Calendar now = Calendar.getInstance();
            if (now.after(calendar)) {
                calendar.add(Calendar.DATE, 1);
            }
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyCleanPendingIntent);
            isDailyCleanFail = false;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Log.d(TAG, "onAccessibilityEvent: \n" + accessibilityEvent + "\n" + currentTargetPackage + "\n" + currentForegroundPackage);
        coreService = this;
        checkDailyClean();
        if (accessibilityEvent.getPackageName() == null
                || (accessibilityEvent.getPackageName().equals(getPackageName())
                && (accessibilityEvent.getClassName().equals("android.view.ViewGroup")
                || accessibilityEvent.getClassName().equals("android.app.AlertDialog")))) {
            // escape some InteractOut packages
            return;
        }
        if (systemPackages.contains((String) accessibilityEvent.getPackageName())) {
            Log.d(TAG, "onAccessibilityEvent: system package return");
//            if (!currrentClassName.equals("com.android.systemui.volume.VolumeDialogImpl$CustomDialog")
//                    && isCountdownLaunched) {
//                countDownTimer.cancel();
//                NotificationManagerCompat.from(this).cancel(2);
//                isCountdownLaunched = false;
//                Toast.makeText(this, "countdown turned off", Toast.LENGTH_SHORT).show();
//            }
            return;
        }
        String text = String.valueOf(accessibilityEvent.getText());
        if (currentForegroundPackage.contentEquals(accessibilityEvent.getPackageName())) {
            return;
        }
        String tempForegroundPackage = (String) accessibilityEvent.getPackageName();
        if (isInLabMode && tempForegroundPackage.equals(getPackageName() + ".Tutorial")) {
            if (!isOverlayOn && !inTutorialMainPage) {
                launchOverlayWindow();
            }
            Log.d(TAG, "onAccessibilityEvent: Tutorial page");
            return;
        }
        if (text.equals("Application icon")) {
            return;
        }
        if (tempForegroundPackage.contains("inputmethod")) {
            Log.d(TAG, "onAccessibilityEvent: English keyboard return");
            ArrayMap<String, Object> args = new ArrayMap<>();
            args.put("info", "KEYBOARD");
            writeToFile("user_actions", args);
            return;
        }
        // commented following lab study code
//        if (isInLabMode &&
//                (currentForegroundPackage.equals("com.twitter.android")
//                        || currentForegroundPackage.equals("com.teamlava.bubble"))) {
//            // in lab mode the intervention of these two app is control on purpose
//            return;
//        }

//        refreshUseTime();
        updateUseTime(currentForegroundPackage, tempForegroundPackage);
        currentForegroundPackage = tempForegroundPackage;
        retrievePackageUsedTime();
        Log.d(TAG, "onAccessibilityEvent: \n" + packageUsedTime + '\n' + packageInExtraTime);
        boolean isTargetApp = packageUsedTime.containsKey(currentForegroundPackage);
        if (isTargetApp) {
            currentTargetPackage = currentForegroundPackage;
            ArrayMap<String, Object> args = new ArrayMap<>();
            args.put("info", "ENTER_APP");
            args.put("is_target", true);
            args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
            args.put("package_name", CoreService.currentTargetPackage);
            args.put("current_usage_in_ms", packageUsedTime.get(currentForegroundPackage));
            CoreService.coreService.writeToFile("state_changes", args);
            if (!isOverlayOn) {
                doSthWhenEnteringTarget();
            } else {
                closeOverlayWindow();
                if (packageGrantedTime.get(currentTargetPackage) == 0L) {
                    if (packageInExtraTime != null && !packageInExtraTime.get(currentTargetPackage)) launchInterventionWithCode(currentTargetPackage);
                }
            }
        } else {
            ArrayMap<String, Object> args = new ArrayMap<>();
            args.put("info", "ENTER_APP");
            args.put("is_target", false);
            args.put("app_name", CoreService.packageNameMap.get(CoreService.currentForegroundPackage));
            args.put("package_name", CoreService.currentForegroundPackage);
            args.put("current_usage_in_ms", packageUsedTime.get(currentForegroundPackage));
            CoreService.coreService.writeToFile("state_changes", args);
            if (isOverlayOn || isLongpressListenerOn) {
                Log.d(TAG, "onAccessibilityEvent: " + isOverlayOn + ' ' + isLongpressListenerOn);
                closeOverlayWindow();
            }
            if (isCountdownLaunched) {
                countDownTimer.cancel();
                isCountdownLaunched = false;
                Toast.makeText(this, "countdown turned off", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUseTime(String beforeForeground, String nowForeground) {
//        Toast.makeText(this, "before: " + beforeForeground + "; after: " + nowForeground, Toast.LENGTH_SHORT).show();
        long timestamp = System.currentTimeMillis();
        if (packageChosen.contains(beforeForeground) && currentForegroundEnterTime != 0) {
            retrievePackageUsedTime();
            long origin = packageUsedTime.get(beforeForeground);
            packageUsedTime.put(beforeForeground, origin + timestamp - currentForegroundEnterTime);
            storePackageUsedTime();
        }
        if (packageChosen.contains(nowForeground)) {
            currentForegroundEnterTime = timestamp;
        }
    }

    private void doSthWhenEnteringTarget() {
        if (!isInterventionOn) {
            return;
        }
        Log.d(TAG, "doSthWhenEnteringTarget: " + currentTargetPackage);
        long time = 0;
        for (int i = 0; i < packageUsedTime.size(); i++) {
            time += packageUsedTime.valueAt(i);
        }
        if (time > targetTimeFieldStudy) {
            if (currentTargetPackage == null) {
                return;
            }
            if (packageGrantedTime.get(currentTargetPackage) == 0L) {
                if (!packageInExtraTime.get(currentTargetPackage)) {
                    launchInterventionWithCode(currentTargetPackage);
                }
            }
        } else if (!isCountdownLaunched && targetTimeFieldStudy > time) {
            long timeRemain = targetTimeFieldStudy - time;
            countDownTimer = new CountDownTimer(timeRemain, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    Log.d(TAG, "onTick: " + millisUntilFinished / 1000 + "s remaining");
                }

                @Override
                public void onFinish() {
                    launchInterventionWithCode(currentTargetPackage);
                    isCountdownLaunched = false;
                }
            }.start();
            isCountdownLaunched = true;
            Toast.makeText(this, "countdown turned on", Toast.LENGTH_SHORT).show();
        }
    }

    public void launchInterventionWithCode(String currentForegroundPackage) {
        String interventionCode = packageInterventionCode.get(currentForegroundPackage);
        if (Objects.equals(interventionCode, "")) {
            return;
        }
        if (interventionCode.charAt(0) == '1') {
            // default
            launchDefaultIntervention();
        } else {
            // interactout interventions
            setInterventions(interventionCode.charAt(1), interventionCode.charAt(2), currentForegroundPackage);
            launchOverlayWindow();
        }
    }

    public void dailyClean(boolean is_manual) {
        // clean extra permitted time and reset intervention values
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "NEW_DAY");
        args.put("is_manual", is_manual);
        writeToFile("system_events", args);
        ArrayMap<String, Object> usages = new ArrayMap<>();
        packageUsedTime.forEach(usages::put);
        writeToFile("usage", usages);
        resetInterventions();

        for (int i = 0; i < packageUsedTime.size(); i++) {
            semaphore.acquireUninterruptibly();
            packageUsedTime.setValueAt(i, 0L);
            semaphore.release();
        }
        storePackageUsedTime();
        customizeIntervention = false;
        for (int i = 0; i < packageGrantedTime.size(); i++) {
            semaphore.acquireUninterruptibly();
            packageGrantedTime.setValueAt(i, 0L);
            semaphore.release();
        }
        for (int i = 0; i < packageOneMinuteClicked.size(); i++) {
            semaphore.acquireUninterruptibly();
            packageOneMinuteClicked.setValueAt(i, false);
            semaphore.release();
        }
        storePackageOneMinuteClicked();
        if (packageInExtraTime == null) {
            return;
        }
        for (int i = 0; i < packageInExtraTime.size(); i++) {
            semaphore.acquireUninterruptibly();
            packageInExtraTime.setValueAt(i, false);
            semaphore.release();
        }
    }

    public CoreService() {
        super();
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Interrupted");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: unbind");
        closeOverlayWindow();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(checkTimeIntent);
        alarmManager.cancel(dailyCleanPendingIntent);
        interventionUpdateHandler.removeCallbacks(interventionUpdateRunnable);
//        ArrayMap<String, Object> args = new ArrayMap<>();
//        args.put("info", "ACCESSIBILITY_SERVICE_UNBIND");
//        CoreService.coreService.writeToFile("system_events", args);
        unregisterReceiver(broadcastReceiver);
        return super.onUnbind(intent);
    }

    private void clearAllArrays() {
        semaphore.acquireUninterruptibly();
        packageChosen.removeAllElements();
        appChosen.removeAllElements();
        packageUsedTime.clear();
//        packageInterventionCode.clear();
        semaphore.release();
    }

    public void storePackageUsedTime() {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        JSONObject jsonObject = new JSONObject(packageUsedTime);
        edit.putString("usage_time", jsonObject.toString());
        edit.apply();
    }

    public void retrievePackageUsedTime() {
        String stringMap = sharedPreferences.getString("usage_time", null);
        if (stringMap != null) {
            try {
                JSONObject jsonObject = new JSONObject(stringMap);
                Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String pack = iterator.next();
                    Long time = jsonObject.getLong(pack);
                    packageUsedTime.put(pack, time);
                }
            } catch (JSONException e) {
                e.getLocalizedMessage();
            }
        }
    }

    public void storePackageOneMinuteClicked() {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        JSONObject jsonObject = new JSONObject(packageOneMinuteClicked);
        edit.putString("one_min_clicked", jsonObject.toString());
        edit.apply();
    }

    public void retrievePackageOneMinuteClicked() {
        String stringMap = sharedPreferences.getString("one_min_clicked", null);
        if (stringMap != null) {
            try {
                JSONObject jsonObject = new JSONObject(stringMap);
                Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String pack = iterator.next();
                    Boolean clicked = jsonObject.getBoolean(pack);
                    packageOneMinuteClicked.put(pack, clicked);
                }
            } catch (JSONException e) {
                e.getLocalizedMessage();
            }
        }
    }
    public void storeParticipantInfo() {
        if (participantFilename == null || participantDisplayName == null) {
            return;
        }
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString("participant_filename", participantFilename);
        edit.putString("participant_display_name", participantDisplayName);
        edit.apply();
    }

    public void retrieveParticipantInfo() {
        participantFilename = sharedPreferences.getString("participant_filename", null);
        participantDisplayName = sharedPreferences.getString("participant_display_name", null);
    }

    public void syncAppMap() {
        retrieveParticipantInfo();
        if (participantFilename == null) {
            Toast.makeText(this, "Participant Filename is null, please report this error to the study team.", Toast.LENGTH_LONG).show();
            return;
        }
         db.collection("users").document(participantFilename).collection("Settings").document("Interventions").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    Log.d("firebase", String.valueOf(task.getResult().getData()));
                    if (task.getResult().getData() == null) return;
                    clearAllArrays();
                    semaphore.acquireUninterruptibly();
                    task.getResult().getData().forEach((k, v) -> {
                        packageChosen.add(k);
                        appChosen.add(packageNameMap.get(k));
                        packageUsedTime.put(k, 0L);
                        packageGrantedTime.put(k, 0L);
                        packageOneMinuteClicked.put(k, false);
                        // ignore specific intervention for each target app
                        packageInterventionCode.put(k, "");
                    });
                    if (packageInExtraTime == null) {
                        packageInExtraTime = new ArrayMap<>();
                        packageChosen.forEach(pack -> {
                            packageInExtraTime.put(pack, false);
                        });
                    }
                    semaphore.release();
                    retrievePackageUsedTime();
                    retrievePackageOneMinuteClicked();
                    db.collection("users").document(participantFilename).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                Log.d("firebase", String.valueOf(task.getResult().getData()));
                            Map<String, Object> res = task.getResult().getData();
                            if (res == null) return;
                            for (int i = 0; i < packageInterventionCode.size(); i++) {
                                packageInterventionCode.setValueAt(i, res.get("intervention").toString());
                            }
                            Log.d("syncAppMap", "syncAppMap: " + packageChosen);
                            // for study use
                            isInterventionOn = (boolean) res.get("is_intervention_on");
                            targetTimeFieldStudy = (long) res.get("target_time");
                            Log.d("syncAppMap", "syncAppMap: " + packageInterventionCode);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        createNotificationChannel();
        sharedPreferences = getSharedPreferences("packageUsedTime", MODE_PRIVATE);
        if (SignInActivity.displayName != null && SignInActivity.fileName != null && participantDisplayName == null && participantFilename == null) {
            participantDisplayName = SignInActivity.displayName;
            participantFilename = SignInActivity.fileName;
        }
        storeParticipantInfo();
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCustomKey("username", participantFilename);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        tapAssistanceOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, displayMetrics);
        window = new Window(this);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(broadcastReceiver, intentFilter);
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
        FirebaseMessaging.getInstance().subscribeToTopic("Survey").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
            }
        });
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 10;
        this.setServiceInfo(info);
        Log.d(TAG, "onServiceConnected: \n" + screenWidth + ' ' + screenHeight);
        coreService = this;
        Intent heartbeatIntent = new Intent(this, HeartbeatSender.class);
        checkTimeIntent = PendingIntent.getBroadcast(this, 10, heartbeatIntent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 3600000L, checkTimeIntent);
        Intent dailyCleanIntent = new Intent(this, DailyCleanReceiver.class);
        dailyCleanPendingIntent = PendingIntent.getBroadcast(this, 11, dailyCleanIntent, PendingIntent.FLAG_IMMUTABLE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Calendar now = Calendar.getInstance();
        if (now.after(calendar)) {
            calendar.add(Calendar.DATE, 1);
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyCleanPendingIntent);
        syncAppMap();
        interventionUpdateHandler = new Handler();
        interventionUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isOverlayOn) {
                    Log.d(TAG, "run: runtime increase");
                    increaseIntensityByTime(currentTargetPackage);
                }
                interventionUpdateHandler.postDelayed(interventionUpdateRunnable, 60000);
            }
        };

        // get all installed apps
        getAllInstalledApps();
//        updateUseTime();
        Log.d(TAG, "onServiceConnected: Service connected");
    }

    public void resetInterventions() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.cancel(2);
        notificationManagerCompat.cancel(3);
        CoreService.tapDelay = 0;
        CoreService.isDoubleTapToSingleTap = false;
        CoreService.xOffset = 0;
        CoreService.yOffset = 0;
        GestureDetector.TAP_THRESHOLD = 0;
        GestureDetector.LONG_PRESS_PROLONG = 1000;
        CoreService.swipeDelay = 0;
        CoreService.swipeRatio = 1;
        swipeRatioExp = 0;
        CoreService.swipeFingers = 1;
        CoreService.reverseDirection = false;
        for (int i = 0 ; i < tapDelayValues.size(); i++) {
            tapDelayValues.setValueAt(i, 0);
        }
        for (int i = 0; i < tapProlongValues.size(); i++) {
            tapProlongValues.setValueAt(i, 0);
        }
        for (int i = 0; i < swipeDelayValues.size(); i++) {
            swipeDelayValues.setValueAt(i, 0);
        }
        for (int i = 0; i < swipeRatioExpValues.size(); i++) {
            swipeRatioExpValues.setValueAt(i, (double) 0);
        }
    }

    private void resetInterventionLimit() {
        tapDelayMax = 0;
        prolongMax = 0;
        swipeDelayMax = 0;
        scrollRatioMax = 1;
    }

    private void setInterventions(char tapIntervention, char swipeIntervention, String currentForegroundPackage) {
        resetInterventionLimit();
        resetInterventions();
        switch (tapIntervention) {
            case 'a':
                tapDelayMax = 800;
                tapInt = "TAP_DELAY";
                try {
                    tapDelay = tapDelayValues.get(currentForegroundPackage);
                } catch (NullPointerException e) {
                    tapDelay = 0;
                }
                break;
            case 'd':
                prolongMax = 200;
                tapInt = "TAP_PROLONG";
                try {
                    GestureDetector.TAP_THRESHOLD = tapProlongValues.get(currentForegroundPackage);
                } catch (NullPointerException e) {
                    GestureDetector.TAP_THRESHOLD = 0;
                }
                break;
            case 'c':
                isDoubleTapToSingleTap = true;
                tapInt = "DOUBLE_TAP";
                break;
            case 'b':
                yOffset = -100;
                tapInt = "TAP_SHIFT";
                break;
        }

        switch (swipeIntervention) {
            case 'a':
                swipeDelayMax = 800;
                swipeInt = "SWIPE_DELAY";
                try {
                    swipeDelay = swipeDelayValues.get(currentForegroundPackage);
                } catch (NullPointerException e) {
                    swipeDelay = 0;
                }
                break;
            case 'b':
                scrollRatioMax = 4;
                swipeInt = "SWIPE_SCALE";
                try {
                    swipeRatioExp = swipeRatioExpValues.get(currentForegroundPackage);
                    swipeRatio = Math.pow(scrollRatioMax, swipeRatioExp);
                } catch (NullPointerException e) {
                    swipeRatio = 1;
                    swipeRatioExp = 0;
                }
                break;
            case 'c':
                reverseDirection = true;
                swipeInt = "SWIPE_REVERSE";
                break;
            case 'd':
                swipeFingers = 2;
                swipeInt = "SWIPE_MULTI_FINGERS";
                break;
        }
        String currentIntervention = getCurrentInterventions();
        Log.d(TAG, "increaseIntensity: 222");
        broadcastField("Current Interventions for " + packageNameMap.get(currentTargetPackage), currentIntervention, currentTargetPackage, 2, true);
    }

    public void writeToFile(String type, ArrayMap<String, Object> args) {
        retrieveParticipantInfo();
        if (participantFilename == null) {
            Toast.makeText(this, "Participant Filename is null, please report this error to the study team.", Toast.LENGTH_LONG).show();
            return;
        }
        Map<String, Object> data = new ArrayMap<>(args);
        data.put("local_timestamp", System.currentTimeMillis());
        data.put("timestamp", FieldValue.serverTimestamp());
        data.put("version", 5);
        db.collection("users").document(participantFilename).collection("logs_" + type).add(data)
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onWriteFailure: " + e.getLocalizedMessage());
            }
        });
//        SetOptions.merge()
    }

    private void getAllInstalledApps() {
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
//            Log.d("List", packageInfo.packageName + "+" + ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) + "+" + getPackageManager().getApplicationLabel(packageInfo.applicationInfo));
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                packageNameMap.put(packageInfo.packageName, packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
            } else if (!packageInfo.packageName.contains("inputmethod")) {
                systemPackages.add(packageInfo.packageName);
            } else {
                packageNameMap.put(packageInfo.packageName, "KEYBOARD");
            }
        }
        packageNameMap.put("com.google.android.youtube", "YouTube");
        systemPackages.remove("com.google.android.youtube");
        packageNameMap.put("com.google.android.gm", "Gmail");
        systemPackages.remove("com.google.android.gm");
        packageNameMap.put("com.android.chrome", "Chrome");
        systemPackages.remove("com.android.chrome");
        packageNameMap.put("com.netflix.mediaclient", "Netflix");
        systemPackages.remove("com.netflix.mediaclient");
    }

//    public Map<String, UsageStats> getUsageStats(long startTime, long endTime) {
//        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
//        return usageStatsManager.queryAndAggregateUsageStats(
//                startTime, endTime);
//    }

    public void launchOverlayWindow() {
        if (!isOverlayOn) {
            Log.d(TAG, "OverlayWindow: open");
            window.open(this);
            isOverlayOn = true;
            isLongpressListenerOn = false;
            interventionUpdateHandler.postDelayed(interventionUpdateRunnable, 60000);
            Toast.makeText(this, "Overlay window launched", Toast.LENGTH_SHORT).show();
            ArrayMap<String, Object> args = new ArrayMap<>();
            args.put("info", true);
            writeToFile("overlay", args);
        }
    }

    private void increaseIntensity(String currentTargetPackage) {
        if (customizeIntervention) {
            return;
        }
        Log.d(TAG, "increaseIntensity");
        if (tapDelay + 10 <= tapDelayMax) {
            tapDelay += 10;
        }
        if (swipeDelay + 10 <= swipeDelayMax) {
            swipeDelay += 10;
        }
        if (GestureDetector.TAP_THRESHOLD + 10 <= prolongMax) {
            GestureDetector.TAP_THRESHOLD += 10;
        }
        if (swipeRatioExp + 0.01 <= 1) {
            swipeRatioExp += 0.01;
            swipeRatio = Math.pow(scrollRatioMax, swipeRatioExp);
        }
        if (packageChosen.contains(currentTargetPackage)) {
            tapDelayValues.put(currentTargetPackage, tapDelay);
            swipeDelayValues.put(currentTargetPackage, swipeDelay);
            tapProlongValues.put(currentTargetPackage, GestureDetector.TAP_THRESHOLD);
            swipeRatioExpValues.put(currentTargetPackage, swipeRatioExp);
        }
        broadcastField("Current Interventions for " + packageNameMap.get(currentTargetPackage), getCurrentInterventions(), currentTargetPackage, 2, true);
    }
    private void increaseIntensityByTime(String currentTargetPackage) {
        if (customizeIntervention) {
            return;
        }
        Log.d(TAG, "increaseIntensity");
        if (tapDelay + 80 <= tapDelayMax) {
            tapDelay += 80;
        } else {
            tapDelay = tapDelayMax;
        }
        if (swipeDelay + 80 <= swipeDelayMax) {
            swipeDelay += 80;
        } else {
            swipeDelay = swipeDelayMax;
        }
        if (GestureDetector.TAP_THRESHOLD + 20 <= prolongMax) {
            GestureDetector.TAP_THRESHOLD += 20;
        } else {
            GestureDetector.TAP_THRESHOLD = prolongMax;
        }
        if (swipeRatioExp + 0.1 <= 1) {
            swipeRatioExp += 0.1;
            swipeRatio = Math.pow(scrollRatioMax, swipeRatioExp);
        } else {
            swipeRatioExp = 1;
            swipeRatio = scrollRatioMax;
        }
        if (packageChosen.contains(currentTargetPackage)) {
            tapDelayValues.put(currentTargetPackage, tapDelay);
            swipeDelayValues.put(currentTargetPackage, swipeDelay);
            tapProlongValues.put(currentTargetPackage, GestureDetector.TAP_THRESHOLD);
            swipeRatioExpValues.put(currentTargetPackage, swipeRatioExp);
        }
        broadcastField("Current Interventions for " + packageNameMap.get(currentTargetPackage), getCurrentInterventions(), currentTargetPackage, 2, true);
        sendInterventionInformation();
    }

    private void sendInterventionInformation() {
        if (tapInt == null || swipeInt == null) {
            return;
        }
        retrievePackageUsedTime();
        ArrayMap<String, Object> tapArgs = new ArrayMap<>();
        tapArgs.put("info", tapInt);
        switch (tapInt) {
            case "TAP_DELAY":
                tapArgs.put("intensity", tapDelay);
                break;
            case "TAP_PROLONG":
                tapArgs.put("intensity", GestureDetector.TAP_THRESHOLD);
                break;
            case "TAP_DOUBLE":
                tapArgs.put("intensity", isDoubleTapToSingleTap);
                break;
            case "TAP_SHIFT":
                tapArgs.put("intensity", yOffset);
                break;
        }
        tapArgs.put("app_name", packageNameMap.get(currentTargetPackage));
        tapArgs.put("package_name", currentTargetPackage);
        tapArgs.put("current_usage_in_ms", packageUsedTime.get(currentTargetPackage));
        writeToFile("intervention_tap_change", tapArgs);

        ArrayMap<String, Object> swipeArgs = new ArrayMap<>();
        swipeArgs.put("info", swipeInt);
        switch (swipeInt) {
            case "SWIPE_DELAY":
                swipeArgs.put("intensity", swipeDelay);
                break;
            case "SWIPE_SCALE":
                swipeArgs.put("intensity", swipeRatio);
                break;
            case "SWIPE_REVERSE":
                swipeArgs.put("intensity", reverseDirection);
                break;
            case "SWIPE_MULTI_FINGERS":
                swipeArgs.put("intensity", swipeFingers);
                break;
        }
        swipeArgs.put("app_name", packageNameMap.get(currentTargetPackage));
        swipeArgs.put("package_name", currentTargetPackage);
        swipeArgs.put("current_usage_in_ms", packageUsedTime.get(currentTargetPackage));
        writeToFile("intervention_swipe_change", swipeArgs);
    }

    public String getCurrentInterventions() {
        String res = "";
        if (tapDelayMax != 0)
            res += String.format(Locale.ENGLISH, "Your tap is delayed %dms\n\n", CoreService.tapDelay + 200);
        if (prolongMax != 0)
            res += String.format(Locale.ENGLISH, "Hold your finger on the screen for at least %dms to trigger a single tap\n\n", GestureDetector.TAP_THRESHOLD);
        if (isDoubleTapToSingleTap) res += "Double tap to trigger a single tap\n\n";
        if (xOffset != 0 || yOffset != 0)
//            res += String.format(Locale.ENGLISH, "Tap x offset: %ddp; y offset: %ddp\n", CoreService.xOffset, CoreService.yOffset);
//            use ratio of 200dp over screen height to represent distance
            res += String.format(Locale.ENGLISH, "Tap a bit lower the place you want to tap. You can enable Tap Assistance in main page to help you locate your tap\n\n");
        if (swipeDelayMax != 0)
            res += String.format(Locale.ENGLISH, "Your swipe is delayed %dms\n", CoreService.swipeDelay);
        if (scrollRatioMax != 1)
            res += String.format(Locale.ENGLISH, "Your swipe is times x%.2f slower\n", CoreService.swipeRatio);
        if (reverseDirection) res += "Swipe direction reversed\n";
        if (swipeFingers != 1)
            res += String.format(Locale.ENGLISH, "Use at least %d fingers to swipe\n", CoreService.swipeFingers);
        return (!res.equals("")) ? res.substring(0, res.length() - 1) : "Lockout Window";
    }

    private void launchDefaultIntervention() {
        if (isDefaultInterventionLaunched) {
            return;
        }
        isDefaultInterventionLaunched = true;
        Intent intent = new Intent(this, AppBlockPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("package_name", currentTargetPackage);
        startActivity(intent);
    }

    public View launchIgnoreLimitMenu() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1080, (int) height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSPARENT);
        params.gravity = Gravity.BOTTOM;
        params.dimAmount = (float) 0.3;
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View menu = layoutInflater.inflate(R.layout.ignore_limit_select_menu, null);
        menu.setBackgroundColor(Color.parseColor("#00123456"));
        windowManager.addView(menu, params);
        return menu;
    }

    public void closeIgnoreLimitMenu(View menu, String packageToGo) {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.removeView(menu);
        if (packageToGo != null) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageToGo);
            startActivity(intent);
        }
    }


    private void stepIncrease(String currentTargetPackage) {
        if (Objects.equals(currentTargetPackage, "")) {
            return;
        }
        currentStep++;
        if (currentStep == SATURATION_NUM) {
            increaseIntensity(currentTargetPackage);
            currentStep = 0;
        }
        sendInterventionInformation();
    }

    public void closeOverlayWindow() {
        if (isOverlayOn) {
            Log.d(TAG, "OverlayWindow: close");
            window.close();
            isOverlayOn = false;
            isLongpressListenerOn = false;
            interventionUpdateHandler.removeCallbacks(interventionUpdateRunnable);
            Toast.makeText(this, "Overlay window close", Toast.LENGTH_SHORT).show();
            ArrayMap<String, Object> args = new ArrayMap<>();
            args.put("info", false);
            writeToFile("overlay", args);
        }
    }

//    public void addOneInput() {
//        semaphore.acquireUninterruptibly();
//        currentWaitingGestures++;
//        Log.d("currentWaitingGestures_add", currentWaitingGestures + "");
//        semaphore.release();
//    }
//
//    public void subtractOneInput() {
//        semaphore.acquireUninterruptibly();
//        currentWaitingGestures--;
//        Log.d("currentWaitingGestures_sub", currentWaitingGestures + "");
//        semaphore.release();
//    }

    public void performSingleTap(float x, float y, long delay, long duration) {
        x = x + xOffset;
        y = y + yOffset;
        if (isTapAssistance) {
            drawTapPosition(x, y);
        }
        stepIncrease(currentTargetPackage);
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "TAP");
        writeToFile("user_gestures", args);
//        Log.d(TAG, "performSingleTap: \n" + x + ' ' + y);
        if (x < 0 || y < 0) {
            // TODO landscape mode upper bound set.
            Toast.makeText(this, "Offset tap out of bound", Toast.LENGTH_SHORT).show();
//            String content = String.format(Locale.ENGLISH, "SINGLE_TAP_OUT_OF_BOUND;%d;%f;%f\n", System.currentTimeMillis(), x, y);
//            writeToFile(dataFileUri, content);
            return;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        this.dispatchGesture(clickBuilder.build(), null, null);
    }


    public void performSwipe(int numFigure, Vector<Float>[] x, Vector<Float>[] y, long delay, long duration) {
        if (duration == 0) return;
        stepIncrease(currentTargetPackage);
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "SWIPE");
        writeToFile("user_gestures", args);
        if (x.length == 0 || y.length == 0) {
//            String content = String.format(Locale.ENGLISH, "NO_SWIPE_POINTS;%d\n", System.currentTimeMillis());
//            writeToFile(dataFileUri, content);
            return;
        }
//        Log.d(TAG, "performSwipe: \n" + numFigure);
        StringBuilder swipePointers = new StringBuilder();
        Path[] path = new Path[numFigure];
        GestureDescription.StrokeDescription[] swipeStroke = new GestureDescription.StrokeDescription[numFigure];
        GestureDescription.Builder swipeBuilder = new GestureDescription.Builder();
        for (int i = 0; i < numFigure; i++) {
            swipePointers.append(x[i].toString()).append(";").append(y[i].toString()).append(";");
            path[i] = new Path();
            if (reverseDirection) {
                Collections.reverse(x[i]);
                Collections.reverse(y[i]);
            }
            try {
                if (x[i].firstElement() + xOffset < 0 || y[i].firstElement() + yOffset < 0) {
                    Toast.makeText(this, "Offset swipe out of bound", Toast.LENGTH_SHORT).show();
//                    String content = String.format(Locale.ENGLISH, "SWIPE_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
//                    writeToFile(dataFileUri, content);
                    return;
                }
            } catch (NoSuchElementException | IllegalStateException e) {
                return;
            }
            path[i].moveTo(x[i].firstElement() + xOffset, y[i].firstElement() + yOffset);
            for (int j = 1; j < x[i].size(); j++) {
                if (x[i].get(j) + xOffset < 0 || y[i].get(j) + yOffset < 0) {
                    Toast.makeText(this, "Offset swipe out of bound", Toast.LENGTH_SHORT).show();
//                    String content = String.format(Locale.ENGLISH, "SWIPE_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
//                    writeToFile(dataFileUri, content);
                    return;
                }
                path[i].lineTo(x[i].get(j) + xOffset, y[i].get(j) + yOffset);
            }
//            Log.d(TAG, "performSwipe: \n");
            swipeStroke[i] = new GestureDescription.StrokeDescription(path[i], delay, duration);
            swipeBuilder.addStroke(swipeStroke[i]);
        }
        this.dispatchGesture(swipeBuilder.build(), null, null);
    }

    public void performDoubleTap(float x, float y, long delay, long duration, long interval) {
        stepIncrease(currentTargetPackage);
        x = x + xOffset;
        y = y + yOffset;
        if (isTapAssistance) {
            drawTapPosition(x, y);
        }
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "DOUBLE_TAP");
        writeToFile("user_gestures", args);
        if (x < 0 || y < 0) {
            Toast.makeText(this, "Offset double tap out of bound", Toast.LENGTH_SHORT).show();
            return;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.StrokeDescription clickStroke2 = new GestureDescription.StrokeDescription(path, delay + interval, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        clickBuilder.addStroke(clickStroke2);
        this.dispatchGesture(clickBuilder.build(), null, null);
    }

    public void drawTapPosition(float x, float y) {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.baseline_circle_24);
        FrameLayout frameLayout = new FrameLayout(this);
        imageView.setImageAlpha(50);
        frameLayout.addView(imageView);
        Log.d(TAG, "drawTapPosition: " + frameLayout.getHeight() + " " + frameLayout.getWidth());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, (int) x - tapAssistanceOffset, (int) y - tapAssistanceOffset,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT);
        params.gravity = Gravity.LEFT | Gravity.TOP;
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(frameLayout, params);
        Handler cancelDrawableHandler = new Handler();
        cancelDrawableHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                windowManager.removeView(frameLayout);
            }
        }, 1000);
    }

    public void broadcast(String title, String txt, boolean hasIntent) {
        Intent fullScreenIntent = new Intent(this, LabQuiz.class);
        fullScreenIntent.setAction(Intent.ACTION_MAIN);
        fullScreenIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "lab_study")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(txt)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM);
        if (hasIntent) {
            builder = builder.setFullScreenIntent(fullScreenPendingIntent, true);
        }
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManagerCompat.notify(1, builder.build());
    }

    public void broadcastField(String title, String txt, String pack, int id, boolean isOngoing) {
        // make a ongoing notification
        Log.d(TAG, "broadcastField: " + pack);
        if (pack == null || !packageChosen.contains(pack) || packageInExtraTime.get(pack)) {
            Log.d(TAG, "broadcastField: returned");
            return;
        }
        Intent oneMinuteIntent = new Intent(this, InterventionActionReceiver.class);
        oneMinuteIntent.putExtra("extra time", 60000L);
        PendingIntent oneMinutePendingIntent = PendingIntent.getBroadcast(this, 1, oneMinuteIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent fifteenMinuteIntent = new Intent(this, InterventionActionReceiver.class);
        fifteenMinuteIntent.putExtra("extra time", 15 * 60000L);
        PendingIntent fifteenMinutePendingIntent = PendingIntent.getBroadcast(this, 2, fifteenMinuteIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent forTodayIntent = new Intent(this, InterventionActionReceiver.class);
        forTodayIntent.putExtra("extra time", 86400000L);
        PendingIntent forTodayPendingIntent = PendingIntent.getBroadcast(this, 3, forTodayIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "field_study")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSilent(true);
        retrievePackageOneMinuteClicked();
        if (!packageOneMinuteClicked.get(pack)) {
            builder.addAction(R.drawable.ic_launcher_foreground, "One more minute", oneMinutePendingIntent);
        }
        builder.addAction(R.drawable.ic_launcher_foreground, "15 more minute", fifteenMinutePendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Ignore for today", forTodayPendingIntent);
        if (isOngoing) {
            builder.setOngoing(true).setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(txt));
        } else {
            builder.setContentText(txt);
        }

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "please give notification permission", Toast.LENGTH_LONG).show();
            return;
        }
        notificationManagerCompat.notify(id, builder.build());
    }

    public void broadcastInStudySurvey(String title, String txt) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "in_study_survey")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(txt)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true).setAutoCancel(true);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/QVYkzz6Ti99YyweM7"));
//        intent.setClass(this, CancelNotification.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setFullScreenIntent(pendingIntent, true);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "please give notification permission", Toast.LENGTH_LONG).show();
            return;
        }
        notificationManagerCompat.notify(3, builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = "Lab Study";
        String description = "Lab study channel";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("lab_study", name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationChannel field = new NotificationChannel("field_study", "Field Study", importance);
        field.setDescription("Field study channel");
        notificationManager.createNotificationChannel(field);

        NotificationChannel survey = new NotificationChannel("in_study_survey", "In-Study Survey", importance);
        field.setDescription("In-study survey channel");
        notificationManager.createNotificationChannel(survey);
    }
}

