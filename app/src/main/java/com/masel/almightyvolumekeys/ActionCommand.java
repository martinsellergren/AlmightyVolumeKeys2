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
    private int deviceStateOnCommandStart;

    private AudioStreamState resetAudioStreamState = null;

    ActionCommand(MyContext myContext) {
        this.myContext = myContext;
    }

    /**
     * Adds a command-bit to the command.
     */
    void addBit(int volumePress, AudioStreamState resetAudioStreamState) {
        handler.removeCallbacksAndMessages(null);

        if (command.length() == 0) {
            this.deviceStateOnCommandStart = myContext.deviceState.getCurrent();
            this.resetAudioStreamState = resetAudioStreamState;
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
        if (myContext.deviceState.getCurrent() != deviceStateOnCommandStart) {
            reset();
            return;
        }

        Action action = getGlobalAction(command);
        if (action == null) action = getMappedAction(command);

        if (action == null || action.getName().equals((new Actions.No_action().getName()))) {
            RecUtils.log(String.format("Non-mapped command: %s (state:%s)", command, DeviceState.str(myContext.deviceState.getCurrent())));
            reset();
            return;
        }

        RecUtils.log(String.format("Execute %s -> %s (state:%s)", command, action.getName(), DeviceState.str(myContext.deviceState.getCurrent())));

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

    private Action getGlobalAction(String command) {
        if (resetAudioStreamState == null) return null;
        int minVolume = myContext.volumeUtils.getMin(resetAudioStreamState.getStream());
        int maxVolume = myContext.volumeUtils.getMax(resetAudioStreamState.getStream());
        int startVolume = resetAudioStreamState.getVolume();

        if (command.equals("111") && startVolume == maxVolume - 1) {
            return resetAudioStreamState.getStream() == AudioManager.STREAM_MUSIC ? new Actions.Media_volume_100() : new Actions.Ringtone_volume_100();
        }
        if (command.equals("000") && startVolume == minVolume + 1) {
            return resetAudioStreamState.getStream() == AudioManager.STREAM_MUSIC ? new Actions.Media_volume_0() : new Actions.Ringtone_volume_0();
        }

        return null;
    }

    private Action getMappedAction(String command) {
        int state = myContext.deviceState.getCurrent();
        String key = String.format("mappingListPreference_%s_command_%s", DeviceState.str(state).toLowerCase(), command);

        String actionName = myContext.sharedPreferences.getString(key, null);
        if (actionName == null) return null;
        Action action = Actions.getActionFromName(actionName);

        if (action instanceof MultiAction) {
            ((MultiAction) action).setAction(myContext);
        }

        return action;
    }

    private void discardAnyVolumeChanges() {
        if (resetAudioStreamState != null) {
            RecUtils.log(resetAudioStreamState.toString());
            myContext.volumeUtils.setVolume(resetAudioStreamState.getStream(), resetAudioStreamState.getVolume(), false);
        }
    }

    /**
     * Remove all mappings of action
     */
    private void unmapAction(Action action) {
        for (Map.Entry<String, String> entry : Utils.getMappings(myContext.sharedPreferences)) {
            if (entry.getValue().equals(action.getName())) {
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

    /**
     * @return True if any mapped command starts with current command.
     */
    boolean isMappedCommandStart() {
        for (Map.Entry<String, String> mapping : Utils.getMappings(myContext.sharedPreferences)) {
            if (!mapping.getValue().equals(new Actions.No_action().getName())) {
                String completeKey = mapping.getKey();
                String prefixKey = String.format("mappingListPreference_%s_command_%s", DeviceState.str(deviceStateOnCommandStart).toLowerCase(), command);
                if (completeKey.startsWith(prefixKey)) return true;
            }
        }
        return false;
    }
}
