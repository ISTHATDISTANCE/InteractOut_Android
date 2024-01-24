package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;

import java.util.Locale;
import java.util.Vector;

public class PanelActivity extends AppCompatActivity  {

    public static Vector<String> packageToSetTime = new Vector<>();

    private static final String TAG = "Main Panel";
    private TextView declaredTimeDisplay;

    private View.OnClickListener appChoices, stop;
    private AlertDialog alertDialog;
    private boolean isLaunched = false;
    private Chip chipTapDelay, chipProlong, chipDouble, chipOffset;
    private Chip chipSwipeDelay, chipRatio, chipReverse, chipMultiple;

    private void promptAccessibilitySettingNote() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel);

        ContentResolver contentResolver = getContentResolver();
        Log.d(TAG, "onCreate: accessibility status is " + Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED));
        String status = Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        if (status.equals("0")) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

        declaredTimeDisplay = findViewById(R.id.declaredTimeDisplay);

        Button startApp = findViewById(R.id.startApp);
        startApp.setText("Choose Apps");
        appChoices = new View.OnClickListener() {
            private final Vector<String> app = new Vector<>(), packages = new Vector<>();
            @Override
            public void onClick(View v) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                }
                else {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                    alertBuilder.setTitle("Choose target apps to intervene:");
                    String[] displayApps = CoreService.packageNameMap.values().toArray(new String[0]);
                    alertBuilder.setMultiChoiceItems(displayApps, null, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            if (isChecked) {
                                app.add(CoreService.packageNameMap.valueAt(which));
                                packages.add(CoreService.packageNameMap.keyAt(which));
                                packageToSetTime.add(CoreService.packageNameMap.keyAt(which));
                            } else {
                                app.remove(CoreService.packageNameMap.valueAt(which));
                                packages.remove(CoreService.packageNameMap.keyAt(which));
                                packageToSetTime.remove(CoreService.packageNameMap.keyAt(which));
                            }
                            Log.d(TAG, "onClick: " + which + " is clicked");
                        }
                    });
                    alertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (app.size() == 0) {
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                                alertBuilder.setTitle("Warning");
                                alertBuilder.setItems(new String[]{"Please choose at least one app to continue!"}, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        alertDialog.dismiss();
                                    }
                                });
                                alertDialog = alertBuilder.create();
                                alertDialog.show();
                                startApp.setText("Choose Apps");
                                startApp.setOnClickListener(appChoices);
                                return;
                            } else if (isLaunched) {
                                Toast.makeText(PanelActivity.this, "Already launched!", Toast.LENGTH_SHORT).show();
                            }
                            startApp.setText("Stop");
                            startApp.setOnClickListener(stop);
                            allowTapToSeeApps();
                            CoreService.appChosen = app;
                            CoreService.packageChosen = packages;
                            String currentInterventions = CoreService.coreService.getCurrentInterventions();
                            CoreService.coreService.broadcastField("Current Intervention", currentInterventions, CoreService.currentForegroundPackage, 2, true);
                            alertDialog.dismiss();
                            startActivity(new Intent(PanelActivity.this, TimeSetting.class));
                        }
                    });
                    alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.cancel();
                        }
                    });
                    alertDialog = alertBuilder.create();
                    alertDialog.show();
                }
            }
        };
        stop = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLaunched = false;
                CoreService.coreService.resetInterventions();
                CoreService.coreService.closeOverlayWindow();
                CoreService.appChosen.removeAllElements();
                CoreService.packageChosen.removeAllElements();
                while (!CoreService.packageUsedTime.isEmpty()) {
                    CoreService.packageUsedTime.removeAt(0);
                }
                while (!CoreService.appTargetTime.isEmpty()) {
                    CoreService.appTargetTime.removeAt(0);
                }
                startApp.setText("Choose Apps");
                startApp.setOnClickListener(appChoices);
            }
        };
        startApp.setOnClickListener(appChoices);


        // set chips

        chipTapDelay = findViewById(R.id.chipTapDelay);
        chipProlong = findViewById(R.id.chipProlong);
        chipDouble = findViewById(R.id.chipDouble);
        chipOffset = findViewById(R.id.chipOffset);
        chipSwipeDelay = findViewById(R.id.chipSwipeDelay);
        chipRatio = findViewById(R.id.chipRatio);
        chipReverse = findViewById(R.id.chipReverse);
        chipMultiple = findViewById(R.id.chipMultiple);

        chipTapDelay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipTapDelay.setChecked(!isChecked);
                    return;
                }
                TextView tapDelayDes = findViewById(R.id.tapDelayDescription);
                if (isChecked) {
                    CoreService.tapDelayMax = 800;
                    tapDelayDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.tapDelayMax = 0;
                    tapDelayDes.setVisibility(View.INVISIBLE);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });

        chipProlong.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipProlong.setChecked(!isChecked);
                    return;
                }
                TextView tapProlongDes = findViewById(R.id.tapProlongDescription);
                if (isChecked) {
                    CoreService.prolongMax = 200;
                    tapProlongDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.prolongMax = 0;
                    tapProlongDes.setVisibility(View.INVISIBLE);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });

        chipDouble.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipDouble.setChecked(!isChecked);
                    return;
                }
                TextView tapDoubleDes = findViewById(R.id.tapDoubleDescription);
                if (isChecked) {
                    CoreService.isDoubleTapToSingleTap = true;
                    tapDoubleDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.isDoubleTapToSingleTap = false;
                    tapDoubleDes.setVisibility(View.INVISIBLE);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });

        chipOffset.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipOffset.setChecked(!isChecked);
                    return;
                }
                TextView tapOffsetDes = findViewById(R.id.tapOffsetDescription);
                if (isChecked) {
                    CoreService.yOffset = -200;
                    tapOffsetDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.yOffset = 0;
                    tapOffsetDes.setVisibility(View.INVISIBLE);
                }
                CoreService.xOffset = 0;
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });

        chipSwipeDelay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipSwipeDelay.setChecked(!isChecked);
                    return;
                }
                TextView swipeDelayDes = findViewById(R.id.swipeDelayDescription);
                if (isChecked) {
                    CoreService.swipeDelayMax = 800;
                    swipeDelayDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.swipeDelayMax = 0;
                    swipeDelayDes.setVisibility(View.INVISIBLE);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });

        chipRatio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipRatio.setChecked(!isChecked);
                    return;
                }
                TextView swipeRatioDes = findViewById(R.id.swipeRatioDescription);
                if (isChecked) {
                    CoreService.scrollRatioMax = 4;
                    swipeRatioDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.scrollRatioMax = 1;
                    swipeRatioDes.setVisibility(View.INVISIBLE);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });

        chipReverse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipReverse.setChecked(!isChecked);
                    return;
                }
                TextView swipeReverseDes = findViewById(R.id.swipeReverseDescription);
                if (isChecked) {
                    CoreService.reverseDirection = true;
                    swipeReverseDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.reverseDirection = false;
                    swipeReverseDes.setVisibility(View.INVISIBLE);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });

        chipMultiple.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
                    promptAccessibilitySettingNote();
                    chipMultiple.setChecked(!isChecked);
                    return;
                }
                TextView swipeMultipleDes = findViewById(R.id.swipeMultipleDescription);
                if (isChecked) {
                    CoreService.swipeFingers = 2;
                    swipeMultipleDes.setVisibility(View.VISIBLE);
                } else {
                    CoreService.swipeFingers = 1;
                    swipeMultipleDes.setVisibility(View.INVISIBLE);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, CoreService.currentForegroundPackage, 2, true);
            }
        });


    }

    private void allowTapToSeeApps() {
        declaredTimeDisplay.setText("Tap to see the apps you have chosen.");
        declaredTimeDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                alertBuilder.setTitle("Apps to get away");
                Vector<String> appAndTime = new Vector<>();
                CoreService.packageChosen.forEach(s -> {
                    long time = CoreService.packageUsedTime.get(s);
                    long hour = time / 3600000;
                    time -= hour * 3600000;
                    long minute = time / 60000;
                    time -= minute * 60000;
                    long second = time / 1000;
                    appAndTime.add(String.format(Locale.ENGLISH, "%s: %d hours, %d minutes, %d seconds", CoreService.packageNameMap.get(s), hour, minute, second));
                });
                alertBuilder.setItems(appAndTime.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog = alertBuilder.create();
                alertDialog.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (isLaunched)  {
            CoreService.coreService.disableSelf();
            isLaunched = false;
        }
        super.onDestroy();
    }

    public void settingButtonOnClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void labModeButtonOnClick(View v) {
        Intent intent = new Intent(this, LabMode.class);
        startActivity(intent);
    }



}