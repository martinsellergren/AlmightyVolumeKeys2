package com.masel.almightyvolumekeys;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;

import com.masel.rec_utils.RecUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Defines actions.
 */
class Actions {

    private Actions() {}

    static class No_action extends Action {
        @Override
        String getName() {
            return "No action";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {}

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.NEVER;
        }
    }

    // region Audio recording

    static class Sound_recorder_Start extends Action {
        @Override
        String getName() {
            return "Sound recorder: Start";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            if (TheSoundRecorderConnection.appIsInstalled(myContext.context)) {
                myContext.audioRecorder.start();
            }
            else {
                RecUtils.openAppOnPlayStore(myContext.context, "com.masel.thesoundrecorder2");
                throw new ExecutionException("You need to install The Sound Recorder");
            }
        }

        @Override
        Action.NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (TheSoundRecorderConnection.appIsInstalled(context)) {
                if (Build.VERSION.SDK_INT >= 23)
                    return new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NOTIFICATION_POLICY};
                else
                    return new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            }
            else {
                return new String[]{};
            }
        }
    }

    static class Sound_recorder_Stop_and_save extends Action {
        @Override
        String getName() {
            return "Sound recorder: Stop and save";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            myContext.audioRecorder.stopAndSave();
        }

        @Override
        Action.NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.ON;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23)
                return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            else
                return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }

        @Override
        boolean isAvailable(Context context) {
            return TheSoundRecorderConnection.appIsInstalled(context);
        }
    }

    static class Sound_recorder_Stop_and_trash extends Action {
        @Override
        String getName() {
            return "Sound recorder: Stop and trash";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            myContext.audioRecorder.stopAndTrash();
        }

        @Override
        Action.NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23)
                return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            else
                return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }

        @Override
        boolean isAvailable(Context context) {
            return TheSoundRecorderConnection.appIsInstalled(context);
        }
    }

    // endregion

    // region Media control

    static class Media_Next extends Action {
        @Override
        String getName() {
            return "Media: Next";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_NEXT);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class Media_Previous extends Action {
        @Override
        String getName() {
            return "Media: Previous";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class Media_Previous_x2 extends Action {
        @Override
        String getName() {
            return "Media: Previous x2";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class Media_Play extends Action {
        @Override
        String getName() {
            return "Media: Play";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PLAY);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class Media_Pause extends Action {
        @Override
        String getName() {
            return "Media: Pause";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PAUSE);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    private static void mediaClick(MyContext myContext, int keycode) {
        if (Build.VERSION.SDK_INT < 19) return;
        myContext.audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keycode));
        myContext.audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keycode));
    }

    // endregion

    // region Announce tune

    static class Voice_Announce_tune_title extends Action {
        @Override
        String getName() {
            return "Voice: Announce tune (title)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.tuneAnnouncer.announceTitle();
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Announce_tune_title_and_artist extends Action {
        @Override
        String getName() {
            return "Voice: Announce tune (title and artist)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.tuneAnnouncer.announceTitleAndArtist();
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Announce_tune_title_artist_and_album extends Action {
        @Override
        String getName() {
            return "Voice: Announce tune (title, artist and album)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.tuneAnnouncer.announceTitleArtistAndAlbum();
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    // endregion

    // region Sound mode

    static class Sound_mode_Sound extends Action {
        @Override
        String getName() {
            return "Sound mode: Sound";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return soundModePermission();
        }
    }

    static class Sound_mode_Sound_volume_100 extends Action {
        @Override
        String getName() {
            return "Sound mode: Sound (volume 100%)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_RING, 100, true);
            myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_NOTIFICATION, 100, true);
            myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return soundModePermission();
        }
    }

    static class Sound_mode_Vibrate extends Action {
        @Override
        String getName() {
            return "Sound mode: Vibrate";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return soundModePermission();
        }
    }

    static class Sound_mode_Silent extends Action {
        @Override
        String getName() {
            return "Sound mode: Silent";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.ON;
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return soundModePermission();
        }
    }

    static class Sound_mode_Sound_Vibrate extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_VIBRATE: return new Sound_mode_Sound();
                case AudioManager.RINGER_MODE_NORMAL: return new Sound_mode_Vibrate();
                default: return new Sound_mode_Sound();
            }
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return soundModePermission();
        }
    }

    static class Sound_mode_Sound_Silent extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: return new Sound_mode_Sound();
                case AudioManager.RINGER_MODE_NORMAL: return new Sound_mode_Silent();
                default: return new Sound_mode_Sound();
            }
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return soundModePermission();
        }
    }

    static class Sound_mode_Vibrate_Silent extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: return new Sound_mode_Vibrate();
                case AudioManager.RINGER_MODE_VIBRATE: return new Sound_mode_Silent();
                default: return new Sound_mode_Vibrate();
            }
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return soundModePermission();
        }
    }

    private static String[] soundModePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
        }
        else return new String[]{};
    }

    // endregion

    // region DND mode

    static class Do_not_disturb_On extends Action {

        @Override
        String getName() {
            return "Do not disturb: On";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            RecUtils.setDnd(myContext.notificationManager, true);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.ON;
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    static class Do_not_disturb_Off extends Action {
        @Override
        String getName() {
            return "Do not disturb: Off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            RecUtils.setDnd(myContext.notificationManager, false);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    static class Do_not_disturb_On_Off extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            if (RecUtils.dndIsOn(myContext.notificationManager)) return new Do_not_disturb_Off();
            else return new Do_not_disturb_On();
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    // endregion

    // region Flashlight

    static class Flashlight_On extends Action {
        @Override
        String getName() {
            return "Flashlight: On";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean success = myContext.flashlight.turnOn();
            if (!success) throw new ExecutionException("Flashlight error");
        }

        @Override
        boolean isAvailable(Context context) {
            return new MyFlashlight(context).isAvailable();
        }
    }

    static class Flashlight_Off extends Action {
        @Override
        String getName() {
            return "Flashlight: Off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean success = myContext.flashlight.turnOff();
            if (!success) throw new ExecutionException("Flashlight error");
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        boolean isAvailable(Context context) {
            return new MyFlashlight(context).isAvailable();
        }
    }

    static class Flashlight_On_Off extends MultiAction {

        @Override
        Action pickAction(MyContext myContext) {
            return myContext.flashlight.isOn() ? new Flashlight_Off() : new Flashlight_On();
        }

        @Override
        boolean isAvailable(Context context) {
            return new MyFlashlight(context).isAvailable();
        }
    }

    // endregion

    // region Tell time

    static class Voice_Tell_time extends Action {
        @Override
        String getName() {
            return "Voice: Tell time";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, -1);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Tell_time_volume_100 extends Action {
        @Override
        String getName() {
            return "Voice: Tell time (volume 100%)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, 100);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Tell_time_volume_75 extends Action {
        @Override
        String getName() {
            return "Voice: Tell time (volume 75%)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, 75);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Tell_time_volume_50 extends Action {
        @Override
        String getName() {
            return "Voice: Tell time (volume 50%)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, 50);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    private static void tellTime(MyContext myContext, int volumePercentage, boolean alsoDate) throws Action.ExecutionException {
        String currentTime = new SimpleDateFormat("HH:mm" + (alsoDate ? ", MM/dd/yyyy" : ""), Locale.getDefault()).format(new Date());
        boolean ok = myContext.voice.speak(currentTime, volumePercentage);
        if (!ok) throw new Action.ExecutionException("Text-to-speech error");
    }
    private static void tellTime(MyContext myContext, int volumePercentage) throws Action.ExecutionException {
        tellTime(myContext, volumePercentage, false);
    }

    // endregion

    // region Tell time and date


    static class Voice_Tell_time_and_date extends Action {
        @Override
        String getName() {
            return "Voice: Tell time and date";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, -1, true);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Tell_time_and_date_volume_100 extends Action {
        @Override
        String getName() {
            return "Voice: Tell time and date (volume 100%)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, 100, true);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Tell_time_and_date_volume_75 extends Action {
        @Override
        String getName() {
            return "Voice: Tell time and date (volume 75%)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, 75, true);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    static class Voice_Tell_time_and_date_volume_50 extends Action {
        @Override
        String getName() {
            return "Voice: Tell time and date (volume 50%)";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            tellTime(myContext, 50, true);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.SILENT;
        }
    }

    // endregion

    // region Bluetooth

    static class Bluetooth_On extends Action {
        @Override
        String getName() {
            return "Bluetooth: On";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean ok = MyBluetooth.enable();
            if (!ok) throw new ExecutionException("Bluetooth error");
        }

        @Override
        boolean isAvailable(Context context) {
            return MyBluetooth.isAvailable();
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.ON;
        }
    }

    static class Bluetooth_Off extends Action {
        @Override
        String getName() {
            return "Bluetooth: Off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean ok = MyBluetooth.disable();
            if (!ok) throw new ExecutionException("Bluetooth error");
        }

        @Override
        boolean isAvailable(Context context) {
            return MyBluetooth.isAvailable();
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }
    }

    static class Bluetooth_On_Off extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            if (MyBluetooth.isEnabled()) return new Bluetooth_Off();
            else return new Bluetooth_On();
        }

        @Override
        boolean isAvailable(Context context) {
            return MyBluetooth.isAvailable();
        }
    }

    // endregion

    // region Volumes

    static class Media_volume_100 extends Action {
        @Override
        String getName() {
            return "Media volume: 100%";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_MUSIC, 100, true);
        }
    }

    static class Media_volume_0 extends Action {
        @Override
        String getName() {
            return "Media volume: 0%";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_MUSIC, 0, true);
        }
    }

    static class Ringtone_volume_100 extends Action {
        @Override
        String getName() {
            return "Ringtone volume: 100%";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_RING, 100, true);
        }
    }

    static class Ringtone_volume_0 extends Action {
        @Override
        String getName() {
            return "Ringtone volume: 0%";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_RING, 0, true);
        }
    }

    // endregion

    // region Rotate screen

    static class Screen_orientation_Portrait extends Action {
        @Override
        String getName() {
            return "Screen orientation: Portrait";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_0);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_orientation_Landscape_1 extends Action {
        @Override
        String getName() {
            return "Screen orientation: Landscape 1";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_90);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_orientation_Landscape_2 extends Action {
        @Override
        String getName() {
            return "Screen orientation: Landscape 2";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_270);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_orientation_Portrait_Landscape extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            if (Settings.System.getInt(myContext.context.getContentResolver(), Settings.System.USER_ROTATION, -1) != 0) {
                return new Screen_orientation_Portrait();
            }
            else {
                return new Screen_orientation_Landscape_1();
            }
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_auto_rotate_On extends Action {
        @Override
        String getName() {
            return "Screen auto-rotate: On";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.ON;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_auto_rotate_Off extends Action {
        @Override
        String getName() {
            return "Screen auto-rotate: Off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_auto_rotate_On_Off extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            int status = Settings.System.getInt(myContext.context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, -1);
            if (status == 0) return new Screen_auto_rotate_On();
            if (status == 1) return new Screen_auto_rotate_Off();

            return new No_action();
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    // endregion

    // region Brightness

    static class Screen_brightness_0 extends Action {
        @Override
        String getName() {
            return "Screen brightness: 0%";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_brightness_100 extends Action {
        @Override
        String getName() {
            return "Screen brightness: 100%";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_brightness_0_100 extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            int brightness = Settings.System.getInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
            if (brightness == 0) return new Screen_brightness_100();
            else return new Screen_brightness_0();
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_auto_brightness_On extends Action {
        @Override
        String getName() {
            return "Screen auto-brightness: On";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_auto_brightness_Off extends Action {
        @Override
        String getName() {
            return "Screen auto-brightness: Off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            Settings.System.putInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    static class Screen_auto_brightness_On_Off extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            int mode = Settings.System.getInt(myContext.context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) return new Screen_auto_brightness_On();
            else return new Screen_auto_brightness_Off();
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Settings.ACTION_MANAGE_WRITE_SETTINGS};
        }
    }

    // endregion

    // region More

    static class Switch_keyboard extends Action {
        @Override
        String getName() {
            return "Switch keyboard";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            InputMethodManager imeManager = (InputMethodManager) myContext.context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imeManager == null) throw new ExecutionException("Failed to open keyboard picker");

            myContext.screenOverlay.runAction(myContext.context, imeManager::showInputMethodPicker);
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW};
        }
    }

    static class Show_volume_panel extends Action {
        @Override
        String getName() {
            return "Show volume panel";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            if (Build.VERSION.SDK_INT >= 29) {
                Intent intent = new Intent(Settings.Panel.ACTION_VOLUME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                myContext.context.startActivity(intent);
            }
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 29;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW};
        }
    }

    // endregion

    static class Beep extends Action {
        @Override
        String getName() {
            return "Beep";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        }
    }

    /**
     *
     * @param name As defined in res/values/array.xml, as returned by action.getName().
     * @return Action with specified ~class-name
     */
    static @Nullable Action getActionFromName(@Nullable String name) {
        if (name == null) return null;
        if (name.matches("^Tasker.*")) {
            return new TaskerAction(name);
        }

        name = name.replaceAll("[^a-zA-Z0-9]+", "_");
        name = name.replaceAll("_+$", "");
        name = "com.masel.almightyvolumekeys.Actions$" + name;
        try {
            return (Action)Class.forName(name).newInstance();
        }
        catch (Exception e) {
            return null;
        }
    }
}
