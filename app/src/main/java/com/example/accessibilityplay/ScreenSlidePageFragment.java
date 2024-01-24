package com.example.accessibilityplay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class ScreenSlidePageFragment extends Fragment {
    private int position;
    private View view;
    private static final int[] imgRes = {
            ScreenSlidePagerActivity.swipeImgRes, ScreenSlidePagerActivity.tapImgRes
    };
    private static final String[] titles = {
            ScreenSlidePagerActivity.swipeTitle, ScreenSlidePagerActivity.tapTitle
    };
    private static final String[] descriptions = {
      ScreenSlidePagerActivity.swipeDescription, ScreenSlidePagerActivity.tapDescription
    };
//    private static final int[] imgRes = {
//            R.drawable.swipe_delay,
//            R.drawable.swipe_multiple_fingers,
//            R.drawable.swipe_reverse,
//            R.drawable.swipe_scale,
//            R.drawable.tap_delay,
//            R.drawable.tap_double,
//            R.drawable.tap_prolong,
//            R.drawable.tap_shift
//    };
//    private static final String[] titles = {
//      "Swipe Delay",
//      "Swipe Multiple Fingers",
//      "Swipe Reverse",
//      "Swipe Scale",
//      "Tap Delay",
//      "Tap Double",
//      "Tap Prolong",
//      "Tap Shift"
//    };
//    private static final String[] descriptions = {
//      "This intervention postpones the time for the your swipe to take effect.",
//      "This intervention requires you to use more than one finger (at least 2) to swipe.",
//      "This intervention reverses the direction of the your swipe.",
//      "This intervention changes the replay time of the your swipe. Your swipe will be slower",
//      "This intervention postpones the time for the your tap to take effect.",
//      "This intervention requires double tap to trigger a single tap.",
//      "This intervention only allows the tap with the finger staying on the screen longer than a threshold.",
//      "This intervention shifts the function position away from the actual tap position. You need to tap below the place you want to actually tap."
//    };
    ScreenSlidePageFragment(int position) {
        this.position = position;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(
                R.layout.fragment_screen_slide_page, container, false);
        ImageView imageView = view.findViewById(R.id.interventionGIF);
        Glide.with(this).load(imgRes[position]).into(imageView);
        TextView interventionTitle = view.findViewById(R.id.interventionTitle);
        interventionTitle.setText(titles[position]);
        TextView textView = view.findViewById(R.id.interventionDescription);
        textView.setText(descriptions[position]);
        return view;
    }

}
