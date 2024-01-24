package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

public class AppBlockPage extends AppCompatActivity {
    String packageName;
    String TAG = "AppBlockPage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_block_page);
        packageName = getIntent().getStringExtra("package_name");
        TextView limitText = findViewById(R.id.limitText);
        limitText.setText("You've reached your limit on " + CoreService.packageNameMap.get(packageName));
        Log.d("AppBlockPage", "onCreate: " + packageName);
        Button okBtn = findViewById(R.id.okBtn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okClickHandler();
            }
        });

        TextView ignoreLimitBtn = findViewById(R.id.ignoreLimitTextBtn);
        ignoreLimitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ignoreLimitHandler();
//                AppBlockPage.this.onBackPressed();
            }
        });
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "DEFAULT");
        args.put("intensity", 1);
        args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
        args.put("package_name", CoreService.currentTargetPackage);
        CoreService.coreService.retrievePackageUsedTime();
        args.put("current_usage_in_ms", CoreService.packageUsedTime.get(CoreService.currentTargetPackage));
        CoreService.coreService.writeToFile("intervention_default_change", args);
    }

    public void okClickHandler() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        ArrayMap<String, Object> args = new ArrayMap<>();
        args.put("info", "OK");
        args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
        args.put("package_name", CoreService.currentTargetPackage);
        CoreService.coreService.writeToFile("user_actions", args);
        finish();
    }

    public void ignoreLimitHandler() {
        View menu = CoreService.coreService.launchIgnoreLimitMenu();
        TextView oneMore = menu.findViewById(R.id.oneMoreMinute);
        CoreService.coreService.retrievePackageOneMinuteClicked();
        if (CoreService.packageOneMinuteClicked.get(CoreService.currentTargetPackage)) {
            oneMore.setVisibility(View.GONE);
        } else {
            oneMore.setVisibility(View.VISIBLE);
            oneMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    long oldValue = CoreService.appUsedTime.get(packageName);
//                    CoreService.appGrantedTime.put(packageName, oldValue + 60000L);
//                    long time = 0;
//                    for (int i = 0; i < CoreService.appUsedTime.size(); i++) {
//                        time += CoreService.appUsedTime.valueAt(i);
//                    }
//                    CoreService.targetTimeFieldStudy = time + 60000L;
                    String current = CoreService.currentTargetPackage;
                    CoreService.packageInExtraTime.put(current, true);
                    CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) {
                        @Override
                        public void onTick(long l) {
                            Log.d("TAG", "onTick: "  + l / 1000 + 's' + " " + current);
                        }

                        @Override
                        public void onFinish() {
                            CoreService.packageInExtraTime.put(current, false);
                            if (Objects.equals(current, CoreService.currentForegroundPackage)) {
                                CoreService.coreService.launchInterventionWithCode(current);
                            }
                        }
                    }.start();
                    ArrayMap<String, Object> args = new ArrayMap<>();
                    args.put("info", "MORE_USAGE");
                    args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
                    args.put("package_name", CoreService.currentTargetPackage);
                    args.put("extra_time_in_ms", 60000L);
                    CoreService.coreService.writeToFile("user_actions", args);
                    CoreService.coreService.closeIgnoreLimitMenu(menu, packageName);
                    CoreService.packageOneMinuteClicked.put(CoreService.currentTargetPackage, true);
                    CoreService.coreService.storePackageOneMinuteClicked();
                }
            });
        }
        TextView remindIn15 = menu.findViewById(R.id.remindIn15Min);
        if (CoreService.packageOneMinuteClicked.get(CoreService.currentTargetPackage)) {
            remindIn15.setBackgroundResource(R.drawable.shape_top);
        } else {
            remindIn15.setBackgroundResource(R.drawable.shape_middle);
        }
        remindIn15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String current = CoreService.currentTargetPackage;
                CoreService.packageInExtraTime.put(current, true);
                CountDownTimer countDownTimer = new CountDownTimer(15 * 60000, 1000) {
                    @Override
                    public void onTick(long l) {
                        Log.d("TAG", "onTick: "  + l / 1000 + 's' + " " + current);
                    }

                    @Override
                    public void onFinish() {
                        CoreService.packageInExtraTime.put(current, false);
                        if (Objects.equals(current, CoreService.currentForegroundPackage)) {
                            CoreService.coreService.launchInterventionWithCode(current);
                        }
                    }
                }.start();
                ArrayMap<String, Object> args = new ArrayMap<>();
                args.put("info", "MORE_USAGE");
                args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
                args.put("package_name", CoreService.currentTargetPackage);
                args.put("extra_time_in_ms", 15 * 60000L);
                CoreService.coreService.writeToFile("user_actions", args);
                CoreService.coreService.closeIgnoreLimitMenu(menu, packageName);
            }
        });
        TextView ignore = menu.findViewById(R.id.ignoreForToday);
        ignore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CoreService.packageGrantedTime.put(packageName, 86400000L);
                CoreService.packageInExtraTime.put(CoreService.currentTargetPackage, true);
                ArrayMap<String, Object> args = new ArrayMap<>();
                args.put("info", "MORE_USAGE");
                args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
                args.put("package_name", CoreService.currentTargetPackage);
                args.put("extra_time_in_ms", 86400000L);
                CoreService.coreService.writeToFile("user_actions", args);
                CoreService.coreService.closeIgnoreLimitMenu(menu, packageName);
            }
        });

        TextView cancel = menu.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayMap<String, Object> args = new ArrayMap<>();
                args.put("info", "CANCEL");
                args.put("app_name", CoreService.packageNameMap.get(CoreService.currentTargetPackage));
                args.put("package_name", CoreService.currentTargetPackage);
                CoreService.coreService.writeToFile("user_actions", args);
                CoreService.coreService.closeIgnoreLimitMenu(menu, null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CoreService.isDefaultInterventionLaunched = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}