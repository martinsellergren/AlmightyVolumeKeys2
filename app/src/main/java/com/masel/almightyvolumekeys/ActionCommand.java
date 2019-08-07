package com.masel.almightyvolumekeys;

import android.content.Intent;
import android.os.Handler;

import java.util.Map;

/**
 * Builds a command and executes it.
 * 4 or more volume-presses falls back to volume change, so commands should never be longer than 3 bits.
 */
class ActionCommand {

    /**
     * Max time between added bits before command executed. */
    static final int DELTA_PRESS_TIME_FAST = 1000;
    static final int DELTA_PRESS_TIME_SLOW = 1500;

    /**
     * Current action command under construction. String of bits. 0 means down, 1 means up volume-key-press.*/
    private String command = "";

    private DeviceState deviceStateOnCommandStart = null;

    private Handler handler = new Handler();

    private MyContext myContext;


    ActionCommand(MyContext myContext) {
        this.myContext = myContext;
    }

    /**
     * Adds a command-bit to the command. Executes command if no other bit added before multi-press-time.
     * Execute command: Perform action mapped to command. Mapping depends on current audio-state.
     * If no mapped action registered, does nothing.
     * @param up else down-command-bit
     * @param deltaPressTime If no more bit before this time, execute.
     */
    void addBit(boolean up, int deltaPressTime) {
        command += (up ? "1" : "0");
        if (deviceStateOnCommandStart == null)
            deviceStateOnCommandStart = DeviceState.getCurrent(myContext);

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeCommand();
            }
        }, deltaPressTime);
    }

    private void executeCommand() {
        if (DeviceState.getCurrent(myContext) == deviceStateOnCommandStart) {
            Map<String, Action> mappings = Mappings.get(myContext);
            Action action = mappings.get(command);
            if (action == null) {
                Utils.logAndToast(myContext, String.format("Non-mapped command: %s (state:%s)", command, DeviceState.getCurrent(myContext)));
            } else {
                Utils.logAndToast(myContext, String.format("Execute %s -> %s (state:%s)", command, action.getName(), DeviceState.getCurrent(myContext)));

                try {
                    action.getVibration().vibrate();
                    action.run(myContext);
                }
                catch (Action.ExecutionException e) {
                    new MyVibrator(Action.VIBRATION_PATTERN_ERROR, false).vibrate();
                    Utils.toast(myContext, e.getMessage());

                    if (e.lacksPermission) {
                        Intent intent = new Intent(myContext.context, MainActivity.class);
                        myContext.context.startActivity(intent);
                    }
                    else {
                        throw new RuntimeException("Non-permission error: " + e.getMessage());
                    }
                }
            }
        }

        command = "";
        deviceStateOnCommandStart = null;
    }

    int getLength() {
        return command.length();
    }
}
