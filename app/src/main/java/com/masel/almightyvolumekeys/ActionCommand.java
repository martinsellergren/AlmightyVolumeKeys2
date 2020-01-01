package com.masel.almightyvolumekeys;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;

import androidx.preference.PreferenceManager;

import com.masel.rec_utils.Utils;

import java.util.Map;

/**
 * Builds a command and executes it.
 * 4 or more volume-presses falls back to volume change, so commands should never be longer than 3 bits.
 */
class ActionCommand {

    /**
     * Max time between added bits before command executed. */
    private static final int DELTA_PRESS_TIME = 1000;

    /**
     * Current action command under construction. String of bits. 0 means down, 1 means up volume-key-press.*/
    private String command = "";

    private DeviceState deviceStateOnCommandStart;

    private int musicVolumeOnCommandStart;

    private Handler handler = new Handler();

    private MyContext myContext;

    private UserInteractionWhenScreenOffAndMusic.ManualMusicVolumeChanger manualMusicVolumeChanger = null;

    ActionCommand(MyContext myContext) {
        this.myContext = myContext;
    }

    /**
     * Adds a command-bit to the command. Executes command if no other bit added before multi-press-time.
     * Execute command: Perform action mapped to command. Mapping depends on current audio-state.
     * If no mapped action registered, does nothing.
     * @param up else down-command-bit
     */
    void addBit(boolean up) {
        if (command.length() == 0) {
            deviceStateOnCommandStart = DeviceState.getCurrent(myContext);
            musicVolumeOnCommandStart = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) +
                    (deviceStateOnCommandStart == DeviceState.MUSIC ? (up ? -1 : 1) : 0);
        }
        command += (up ? "1" : "0");

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::executeCommand, DELTA_PRESS_TIME);
    }

    /**
     * Executes current command.
     * Discards any command that started in a different device-state than the current device-state.
     */
    private void executeCommand() {
        if (DeviceState.getCurrent(myContext) == deviceStateOnCommandStart) {
            Action action = getMappedAction(command);

            if (action == null) {
                Utils.log(String.format("Non-mapped command: %s (state:%s)", command, DeviceState.getCurrent(myContext)));
            }
            else {
                if (action instanceof MultiAction) {
                    ((MultiAction) action).setAction(myContext);
                }

                Utils.logAndToast(myContext.context, String.format("Execute %s -> %s (state:%s)", command, action.getName(), DeviceState.getCurrent(myContext)));

                try {
                    if (!Utils.hasPermissions(myContext.context, action.getNeededPermissions())) {
                        throw new Action.ExecutionException("Missing permission");
                    }
                    if (!action.isAvailable(myContext.context)) {
                        throw new Action.ExecutionException("Action not available on this device");
                    }

                    if (deviceStateOnCommandStart == DeviceState.MUSIC && manualMusicVolumeChanger != null) {
                        manualMusicVolumeChanger.setVolume(musicVolumeOnCommandStart);
                    }

                    Action.execute(myContext, action);
                }
                catch (Action.ExecutionException e) {
                    myContext.notifier.notify(e.getMessage(), Notifier.VibrationPattern.ERROR, false);

                    if (e.getMessage().equals("Missing permission")) {
                        unmapAction(action);
                        if (Utils.currentlyInForeground()) Utils.gotoMainActivity(myContext.context);
                    }
                }
                catch (Exception e) {
                    myContext.notifier.notify(e.getMessage(), Notifier.VibrationPattern.ERROR, false);
                    Utils.log("Unknown error during action execution: " + action.toString() + "\n" + e.getMessage());
                }
            }
        }

        reset();
    }

    void reset() {
        command = "";
        handler.removeCallbacksAndMessages(null);
    }

    int getLength() {
        return command.length();
    }

    void setManualMusicVolumeChanger(UserInteractionWhenScreenOffAndMusic.ManualMusicVolumeChanger manualMusicVolumeChanger) {
        this.manualMusicVolumeChanger = manualMusicVolumeChanger;
    }

    private Action getMappedAction(String command) {
        String state = DeviceState.getCurrent(myContext).toString().toLowerCase();
        String key = String.format("listPreference_%s_command_%s", state, command);

        String actionName = myContext.sharedPreferences.getString(key, null);
        if (actionName == null) return null;
        return Actions.getActionFromName(actionName);
    }

    /**
     * Remove all mappings of action
     */
    private void unmapAction(Action action) {
        for (Map.Entry<String, ?> entry : myContext.sharedPreferences.getAll().entrySet()) {
            if (entry.getValue().toString().equals(action.getName())) {
                myContext.sharedPreferences.edit().putString(entry.getKey(), "No action").apply();
            }
        }
    }
}
