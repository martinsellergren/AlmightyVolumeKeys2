package com.example.almightyvolumekeys;

import android.os.Handler;
import android.util.Log;

import java.util.Map;

/**
 * Builds a command and executes it.
 * 4 or more volume-presses falls back to volume change, so commands should never be longer than 3 bits.
 */
class ActionCommand {

    /**
     * Current action command under construction. String of bits. 0 means down, 1 means up volume-key-press.
     */
    private String command = "";
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
     */
    void addBit(boolean up) {
        long MULTI_PRESS_MAX_TIME = 1000;
        command += (up ? "1" : "0");

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeCommand();
            }
        }, MULTI_PRESS_MAX_TIME);
    }

    private void executeCommand() {
        Map<String, Action> mappings = Mappings.get(myContext);
        Action action = mappings.get(command);
        if (action == null) {
            Log.i("<ME>", String.format("Non-mapped command: %s (state:%s)", command, DeviceState.getCurrent(myContext)));
        }
        else {
            Log.i("<ME>", String.format("Execute %s -> %s (state:%s)", command, action.getName(), DeviceState.getCurrent(myContext)));
            //action.run();
        }

        command = "";
    }

    int getLength() {
        return command.length();
    }
}
