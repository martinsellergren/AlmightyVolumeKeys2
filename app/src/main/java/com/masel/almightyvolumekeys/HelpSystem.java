package com.masel.almightyvolumekeys;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.masel.rec_utils.RecUtils;

public class HelpSystem {
    private HelpSystem(){}

    // region Long press heads-up

    private static final String LONG_PRESS_HEADS_UP_ACTIVE = "LONG_PRESS_HEADS_UP_ACTIVE";

    static boolean isLongPressHeadsUpActive(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(LONG_PRESS_HEADS_UP_ACTIVE, true);
    }

    static void setLongPressHeadsUpActive(Context context, boolean active) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(LONG_PRESS_HEADS_UP_ACTIVE, active).apply();
    }

    // endregion

    // region Voice heads-up

    private static final String VOICE_HEADS_UP_ACTIVE = "VOICE_HEADS_UP_ACTIVE";

    static boolean isVoiceHeadsUpActive(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(VOICE_HEADS_UP_ACTIVE, true);
    }

    static void setVoiceHeadsUpActive(Context context, boolean active) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(VOICE_HEADS_UP_ACTIVE, active).apply();
    }

    // endregion

    // region Pro unlocked heads-up

    private static final String PRO_UNLOCKED_HEADS_UP_ACTIVE = "PRO_UNLOCKED_HEADS_UP_ACTIVE";

    private static boolean isProUnlockedHeadsUpActive(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PRO_UNLOCKED_HEADS_UP_ACTIVE, true);
    }

    static void setProUnlockedHeadsUpActive(Context context, boolean active) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(PRO_UNLOCKED_HEADS_UP_ACTIVE, active).apply();
    }

    static void showProUnlockedHeadsUpIfAppropriate(AppCompatActivity activity) {
        if (isProUnlockedHeadsUpActive(activity)) {
            RecUtils.showHeadsUpDialog(activity,
                    "Thanks for unlocking pro! \n\nYou have unlimited number of mapped actions.",
                    () -> setProUnlockedHeadsUpActive(activity, false));
        }
    }

    // endregion

//    // region Media extreme volume heads-up
//
//    private static final String MEDIA_EXTREME_VOLUME_HEADS_UP_ACTIVE = "MEDIA_EXTREME_VOLUME_HEADS_UP_ACTIVE";
//
//    static boolean isMediaExtremeVolumeHeadsUpActive(Context context) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        return sharedPreferences.getBoolean(MEDIA_EXTREME_VOLUME_HEADS_UP_ACTIVE, true);
//    }
//
//    static void setMediaExtremeVolumeHeadsUpActive(Context context, boolean active) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        sharedPreferences.edit().putBoolean(MEDIA_EXTREME_VOLUME_HEADS_UP_ACTIVE, active).apply();
//    }
//
//    static void showMediaExtremeVolumeHeadsUp(AppCompatActivity activity) {
//        if (!isMediaExtremeVolumeHeadsUpActive(activity)) return;
//
//        RecUtils.showHeadsUpDialog(activity,
//                "To go to <b>max or min volume</b> when media is playing, you need to <b>tripple-click</b> volume key to go the last volume step, or execute Media volume 0%/100% -action. \n\nMore info in Help.",
//                () -> setMediaExtremeVolumeHeadsUpActive(activity, false));
//    }
//
//    // endregion
}
