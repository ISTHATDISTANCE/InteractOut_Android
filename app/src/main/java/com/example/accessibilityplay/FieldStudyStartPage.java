package com.example.accessibilityplay;

import static android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.locks.Lock;


public class FieldStudyStartPage extends AppCompatActivity {
    private AlertDialog alertDialog;
    private String TAG = "FieldStudyStartPage";
    private LinearLayout appLinearLayout;
    private Button reset;


    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                    Toast.makeText(this, "No notifications", Toast.LENGTH_LONG).show();
                }
            });

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                Toast.makeText(this, "Please turn on Notification", Toast.LENGTH_LONG).show();
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_study_start_page);
        Button userPageBtn = findViewById(R.id.goToUserPageBtn);
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        Log.d(TAG, "onCreate: \n" + point.x + ' ' + point.y);
        CoreService.screenWidth = point.x;
        CoreService.screenHeight = point.y;
        userPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToUserPage();
            }
        });
        Button surveyLink = findViewById(R.id.surveyLink);
        surveyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSurvey();
            }
        });
        appLinearLayout = findViewById(R.id.appOptionLinearLayout);
        appLinearLayout.setGravity(Gravity.CENTER);
        askNotificationPermission();
        Button interventionTutorial = findViewById(R.id.interventionTutorialBtn);
        interventionTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FieldStudyStartPage.this, ScreenSlidePagerActivity.class));
            }
        });
        Switch tapAssistanceSwitch = findViewById(R.id.tapAssistanceSwitch);
        tapAssistanceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                CoreService.isTapAssistance = b;
            }
        });

        reset = findViewById(R.id.usageResetBtn);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CoreService.coreService != null) {
                    CoreService.coreService.dailyClean(true);
                    FieldStudyStartPage.this.onResume();
                    reset.setClickable(false);
                    reset.setAlpha(.5f);
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
                } else {
                    Toast.makeText(FieldStudyStartPage.this, "Reset Button not working because Accessibility Service is gone.", Toast.LENGTH_LONG).show();
                    TextView displayName = findViewById(R.id.declaredTimeDisplay);
                    displayName.setTextColor(Color.parseColor("#ff0000"));
                    displayName.setText("Your accessibility service is killed, please go to settings -> accessibility service -> enable InteractOut");
                }
            }
        });
    }

    private String longToTime(long time) {
        long hour = time / 3600000;
        time -= hour * 3600000;
        long minute = time / 60000;
        String res = "";
        if (hour > 1) {
            res += hour + " hours, ";
        } else if (hour == 1) {
            res += hour + " hour, ";
        }
        if (minute > 1) {
            res += minute + " minutes";
        } else {
            res += minute + " minute";
        }
        return res;
    }


    @Override
    protected void onResume() {
        super.onResume();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(new Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            }
        }
        TextView displayName = findViewById(R.id.declaredTimeDisplay);
        if (CoreService.participantDisplayName != null) {
            displayName.setText("Hi " + CoreService.participantDisplayName + ", below is/are your controlled app(s)");
            displayName.setTextColor(Color.parseColor("#000000"));
        }

        if (CoreService.packageNameMap.size() == 0) {
            promptAccessibilitySettingNote();
        } else {
            appLinearLayout.removeAllViews();
            CoreService.coreService.retrievePackageUsedTime();
            renderAppList();
            long time = 0;
            for (int i = 0; i < CoreService.packageUsedTime.size(); i++) {
                time += CoreService.packageUsedTime.valueAt(i);
            }
            String total = longToTime(CoreService.targetTimeFieldStudy);
            String current = longToTime(time);
            TextView totalTimeDisplay = findViewById(R.id.totalTimeDisplay);
            totalTimeDisplay.setText(Html.fromHtml(getString(R.string.total_time_and_current_time, total, current), Html.FROM_HTML_MODE_LEGACY));
        }
        if (CoreService.coreService != null && CoreService.coreService.db != null && CoreService.participantFilename != null) {
            CoreService.coreService.syncAppMap();
            TextView interventionSchedule = findViewById(R.id.interventionInfo);

            CoreService.coreService.db.collection("users").document(CoreService.participantFilename).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        Log.d("firebase", String.valueOf(task.getResult().getData()));
                        if (task.getResult().getData() == null) {
                            addName();
                            return;
                        }
                        ArrayMap<String, Object> res = new ArrayMap<>();
                        res.putAll(task.getResult().getData());
                        if (!res.containsKey("name")) {
                            addName();
                        }
                        String display = getDisplayText((boolean) res.get("is_intervention_on"), (String) res.get("intervention"));
                        Long week = (Long) res.get("week");
                        Long day = (Long) res.get("day");
                        interventionSchedule.setText(String.format(Locale.ENGLISH, "This week %d, day %d, %s", week, day, display));
                    }
                }
            });
            CoreService.coreService.db.collection("users").document(CoreService.participantFilename).collection("logs_system_events").orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        if (task.getResult() == null) {
                            return;
                        }
                        Timestamp t = null;
                        for (QueryDocumentSnapshot q : task.getResult()) {
                            t = (Timestamp) q.get("timestamp");
                        }
                        if (t == null) {
                            reset.setAlpha(.5f);
                            reset.setClickable(false);
                            return;
                        }
                        Log.d("Query", "onComplete: " + t);
                        long current = System.currentTimeMillis();
                        if (current - t.getSeconds() * 1000 >= 3600000L * 24) {
                            reset.setClickable(true);
                            reset.setAlpha(1f);
                        } else {
                            reset.setAlpha(.5f);
                            reset.setClickable(false);
                        }
                    }
                }
            });
        } else {
            displayName.setTextColor(Color.parseColor("#ff0000"));
            displayName.setText("Your accessibility service is killed, please go to settings -> accessibility service -> enable InteractOut");
        }
    }

    private String getDisplayText(boolean isInterventionOn, String interventionCode) {
        if (!isInterventionOn) {
            return getString(R.string.no_intervention);
        } else if (interventionCode.charAt(0) == '1') {
            return getString(R.string.baseline_intervention);
        }
        switch (interventionCode) {
            case "0aa":
                return getString(R.string.interactout_intervention, "Tap Delay", "Swipe Delay");
            case "0ab":
                return getString(R.string.interactout_intervention, "Tap Delay", "Swipe Scale");
            case "0ba":
                return getString(R.string.interactout_intervention, "Tap Shift", "Swipe Delay");
            default:
                return getString(R.string.interactout_intervention, "Tap Shift", "Swipe Scale");
        }
    }

    private void addName() {
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("name", CoreService.participantDisplayName);
        CoreService.coreService.db.collection("users").document(CoreService.participantFilename).set(args, SetOptions.merge())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.getLocalizedMessage());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: name added successfully");
                    }
                });
    }

    private void renderAppList() {
        RelativeLayout.LayoutParams appListParam =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        appListParam.setMargins(0, 25, 0, 25);
        RelativeLayout.LayoutParams appNameParams = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
        appNameParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        appNameParams.addRule(RelativeLayout.CENTER_VERTICAL);
        appNameParams.leftMargin = 100;
        RelativeLayout.LayoutParams appTimeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100);
        appTimeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        appTimeParams.addRule(RelativeLayout.CENTER_VERTICAL);
        appTimeParams.rightMargin = 100;
        CoreService.packageChosen.forEach((p) -> {
            RelativeLayout appList = new RelativeLayout(appLinearLayout.getContext());
            TextView appName = new TextView(appList.getContext());
            appName.setTextSize(18);
            appName.setText(CoreService.packageNameMap.get(p));
            TextView appTime = new TextView(appList.getContext());
            appTime.setTextSize(16);
            appTime.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            appTime.setText(timeDisplay(CoreService.packageUsedTime.get(p)));
            appList.addView(appName, appNameParams);
            appList.addView(appTime, appTimeParams);
            appLinearLayout.addView(appList, appListParam);
        });
    }

    String timeDisplay(Long t) {
        long hour = t / 3600000;
        t -= 3600000 * hour;
        long min = t / 60000;
        t -= 60000 * min;
        return (hour > 0) ? String.format(Locale.ENGLISH, "%d hour, %d min", hour, min) : String.format(Locale.ENGLISH, "%d min", min);
    }


    private void goToUserPage() {
        Intent intent = new Intent(this, PanelActivity.class);
        startActivity(intent);
    }

    private void startSurvey() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/QVYkzz6Ti99YyweM7"));
        startActivity(intent);
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "CLICK_SURVEY_BUTTON");
        CoreService.coreService.writeToFile("user_actions", args);
    }

    private void promptAccessibilitySettingNote() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Warning");
        alertBuilder.setItems(new String[]{"Please enable accessibility settings!"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertBuilder.setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                alertDialog.dismiss();
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}