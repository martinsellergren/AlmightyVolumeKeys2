package com.masel.almightyvolumekeys;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;

import com.masel.rec_utils.RecUtils;

import java.util.Map;

/**
 * Builds a command and executes it.
 */
class ActionCommand {

    /**
     * Max time between added bits before command executed. */
    private static final int DELTA_PRESS_TIME = 1000;

    static final int VOLUME_PRESS_DOWN = 0;
    static final int VOLUME_PRESS_UP = 1;
    static final int VOLUME_PRESS_LONG_DOWN = 2;
    static final int VOLUME_PRESS_LONG_UP = 3;

    /**
     * Current action command under construction. String of bits. 0 means down, 1 means up volume-key-press.*/
    private String command = "";

    private Handler handler = new Handler();

    private MyContext myContext;

    private VolumeKeyCaptureWhenScreenOffAndMusic.ManualMusicVolumeChanger manualMusicVolumeChanger = null;

    private DeviceState deviceStateOnCommandStart;

    private AudioStreamState resetAudioStreamState = null;

//
//    private int relevantAudioStreamOnCommandStart;
//
//    private int relevantAudioStreamVolumeOnCommandStart;

//    private int musicVolumeOnCommandStart;

    ActionCommand(MyContext myContext) {
        this.myContext = myContext;
    }

    /**
     * Adds a command-bit to the command.
     */
    void addBit(int volumePress, AudioStreamState resetAudioStreamState) {
        handler.removeCallbacksAndMessages(null);

        if (command.length() == 0) {

            deviceStateOnCommandStart = DeviceState.getCurrent(myContext);
            this.resetAudioStreamState = resetAudioStreamState;
//            relevantAudioStreamOnCommandStart = getRelevantAudioStream(deviceStateOnCommandStart);
//            relevantAudioStreamVolumeOnCommandStart = myContext.audioManager.getStreamVolume(relevantAudioStreamOnCommandStart);
//
//            if (VolumeKeyCaptureWhenScreenOffAndMusic.screenOffAndMusic(myContext)) {
//                relevantAudioStreamVolumeOnCommandStart += up ? -1 : 1;
//            }
        }
        command += volumePress;
        handler.postDelayed(this::executeCommand, DELTA_PRESS_TIME);
    }

    /**
     * Halt execution count down.
     */
    void halt() {
        handler.removeCallbacksAndMessages(null);
    }

    void removeLastBit() {
        if (command.length() > 0) command = command.substring(0, command.length()-1);
    }

    /**
     * Executes current command: Perform action mapped to command. Mapping depends on current audio-state.
     * Discards any command that started in a different device-state than the current device-state.
     * Discards any volume changes during command input if valid command.
     */
    private void executeCommand() {
        if (DeviceState.getCurrent(myContext) != deviceStateOnCommandStart) {
            reset();
            return;
        }

        Action action = getMappedAction(command);

        if (action == null || action.getName().equals((new Actions.No_action().getName()))) {
            RecUtils.log(String.format("Non-mapped command: %s (state:%s)", command, DeviceState.getCurrent(myContext)));
            reset();
            return;
        }

        RecUtils.log(String.format("Execute %s -> %s (state:%s)", command, action.getName(), DeviceState.getCurrent(myContext)));

        try {
            if (!RecUtils.hasPermissions(myContext.context, action.getNeededPermissions(myContext.context))) {
                throw new Action.ExecutionException("Missing permission");
            }
            if (!action.isAvailable(myContext.context)) {
                throw new Action.ExecutionException("Action not available on this device");
            }

            discardAnyVolumeChanges();
            Action.execute(myContext, action);
        }
        catch (Action.ExecutionException e) {
            myContext.notifier.notify(e.getMessage(), Notifier.VibrationPattern.ERROR, false);
            RecUtils.log(e.getMessage());

            if (e.getMessage().equals("Missing permission")) {
                fixPermissions(action.getNeededPermissions(myContext.context));
            }
        }
        catch (Exception e) {
            myContext.notifier.notify(e.getMessage(), Notifier.VibrationPattern.ERROR, false);
            RecUtils.log("Unknown error during action execution: " + action.toString() + "\n" + e.getMessage());
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

    void setManualMusicVolumeChanger(VolumeKeyCaptureWhenScreenOffAndMusic.ManualMusicVolumeChanger manualMusicVolumeChanger) {
        this.manualMusicVolumeChanger = manualMusicVolumeChanger;
    }

    private Action getMappedAction(String command) {
        DeviceState state = DeviceState.getCurrent(myContext);
        String key = String.format("mappingListPreference_%s_command_%s", state.toString().toLowerCase(), command);

        String actionName = myContext.sharedPreferences.getString(key, null);
        if (actionName == null) return null;
        Action action = Actions.getActionFromName(actionName);

        if (action instanceof MultiAction) {
            ((MultiAction) action).setAction(myContext);
        }

        return action;
    }

    private void discardAnyVolumeChanges() {
        if (resetAudioStreamState == null) return;

        if (deviceStateOnCommandStart == DeviceState.MUSIC && manualMusicVolumeChanger != null) {
            manualMusicVolumeChanger.setVolume(resetAudioStreamState.getVolume());
        }
        else {
            resetAudioStreamState.commit_noUi(myContext);
        }
    }

    /**
     * Remove all mappings of action
     */
    private void unmapAction(Action action) {
        for (Map.Entry<String, ?> entry : myContext.sharedPreferences.getAll().entrySet()) {
            if (entry.getValue().toString().equals(action.getName())) {
                myContext.sharedPreferences.edit().putString(entry.getKey(), new Actions.No_action().getName()).apply();
            }
        }
    }

    private void fixPermissions(String[] permissions) {
        Intent intent = new Intent(myContext.context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_PERMISSION_REQUEST, permissions);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        myContext.context.startActivity(intent);
    }

    private int getRelevantAudioStream(DeviceState state) {
        int activeStream = RecUtils.getActiveAudioStream(myContext.audioManager);
        if (activeStream != AudioManager.USE_DEFAULT_STREAM_TYPE) {
            return activeStream;
        }
        else {
            return Utils.loadVolumeKeysAudioStream(myContext);
        }
    }
}
