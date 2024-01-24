package com.example.accessibilityplay;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

public class ScreenSlidePagerActivity extends FragmentActivity {
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 2;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager2 viewPager;

    private SmallTutorialWindow window;
    public static boolean isOverlayOn = false;
    public static int swipeImgRes, tapImgRes;
    public static String swipeTitle, tapTitle;
    public static String swipeDescription, tapDescription;
    private ScrollView scrollView;

    private int num = 0;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);
        CoreService.isInTutorial = true;
        // Instantiate a ViewPager2 and a PagerAdapter.
        WormDotsIndicator wormDotsIndicator = findViewById(R.id.worm_dots_indicator);
        viewPager = findViewById(R.id.pager);
        String interventionCode = CoreService.packageInterventionCode.valueAt(0);
        Log.d("Intervention Code", "onCreate: " + interventionCode);
        switch (interventionCode.charAt(1)) {
            case 'a':
                tapImgRes = R.drawable.tap_delay;
                tapTitle = "Tap Delay";
                tapDescription = "This intervention postpones the time for the your tap to take effect.";
                break;
            case 'b':
                tapImgRes = R.drawable.tap_shift;
                tapTitle = "Tap Shift";
                tapDescription = "This intervention shifts the function position away from the actual tap position. You need to tap below the place you want to actually tap. \n\nYou are recommended to turn on Tap Assistance to help you locate your tap position.";
                break;
        }
        switch (interventionCode.charAt(2)) {
            case 'a':
                swipeImgRes = R.drawable.swipe_delay;
                swipeTitle = "Swipe Delay";
                swipeDescription = "This intervention postpones the time for the your swipe to take effect.";
                break;
            case 'b':
                swipeImgRes = R.drawable.swipe_scale;
                swipeTitle = "Swipe Scale";
                swipeDescription = "This intervention changes the replay time of the your swipe. Your swipe will be slower";
                break;
        }
        switch (interventionCode) {
            case "0aa":
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        reset();
                        switch (position) {
                            case 0:
                                CoreService.swipeDelay = 800;
                                break;
                            case 1:
                                CoreService.tapDelay = 800;
                                break;
                        }
                    }
                });
                break;
            case "0ba":
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        reset();
                        switch (position) {
                            case 0:
                                CoreService.swipeDelay = 800;
                                break;
                            case 1:
                                CoreService.yOffset = -100;
                                break;
                        }
                    }
                });
                break;
            case "0ab":
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        reset();
                        switch (position) {
                            case 0:
                                CoreService.swipeRatio = 4;
                                break;
                            case 1:
                                CoreService.tapDelay = 800;
                                break;
                        }
                    }
                });
                break;
            case "0bb":
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        reset();
                        switch (position) {
                            case 0:
                                CoreService.swipeRatio = 4;
                                break;
                            case 1:
                                CoreService.yOffset = -100;
                                break;
                        }
                    }
                });
                break;
        }
//        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageSelected(int position) {
//                super.onPageSelected(position);
//                reset();
//                switch (position) {
//                    case 0:
//                        // swipe delay
//                        CoreService.swipeDelay = 800;
//                        return;
//                    case 1:
//                        CoreService.swipeFingers = 2;
//                        return;
//                    case 2:
//                        CoreService.reverseDirection = true;
//                        return;
//                    case 3:
//                        CoreService.swipeRatio = 4;
//                        return;
//                    case 4:
//                        CoreService.tapDelay = 800;
//                        return;
//                    case 5:
//                        CoreService.isDoubleTapToSingleTap = true;
//                        return;
//                    case 6:
//                        GestureDetector.TAP_THRESHOLD = 200;
//                        return;
//                    case 7:
//                        CoreService.yOffset = -200;
//                }
//            }
//        });
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        wormDotsIndicator.attachTo(viewPager);
        scrollView = findViewById(R.id.interventionScrollView);
        window = new SmallTutorialWindow(scrollView.getContext());
        Button playBtn = findViewById(R.id.playBtn);
        TextView playText = findViewById(R.id.playText);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                num ++;
                playText.setText(String.format("%d", num));
            }
        });

    }

    private void reset() {
        CoreService.tapDelay = 0; // 500 ms intrinsic delay
        GestureDetector.TAP_THRESHOLD = 0;
        CoreService.yOffset = 0;
        CoreService.isDoubleTapToSingleTap = false;
        CoreService.swipeDelay = 0;
        CoreService.swipeFingers = 1;
        CoreService.swipeRatio = 1;
        CoreService.reverseDirection = false;
    }

    public void launchOverlayWindow(Context context) {
        if (!isOverlayOn) {
            Log.d("TAG", "launchOverlayWindow: " + CoreService.isInTutorial);
            window.open(context);
            isOverlayOn = true;
            CoreService.isLongpressListenerOn = false;
        }
    }

    public void closeOverlayWindow() {
        Log.d("TAG", "closeOverlayWindow: isOverlayOn " + isOverlayOn);
        if (isOverlayOn) {
            window.close();
            isOverlayOn = false;
            CoreService.isLongpressListenerOn = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
        CoreService.isInTutorial = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        reset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        launchOverlayWindow(scrollView.getContext());
    }

    @Override
    protected void onStop() {
        super.onStop();
        CoreService.isInTutorial = false;
        closeOverlayWindow();
        Toast.makeText(this, "Small Tutorial stopped", Toast.LENGTH_SHORT).show();
    }

    public void imageBackButtonClick(View v) {
        super.onBackPressed();
    }

    public void openInFullScreen(View v) {
        v.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public static View view;
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new ScreenSlidePageFragment(position);
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}