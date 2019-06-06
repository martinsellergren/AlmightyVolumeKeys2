package com.example.almightyvolumekeys;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Map;

/**
 * Builds a command and executes it.
 * 4 or more volume-presses falls back to volume change, so commands may never be longer than 3 bits.
 */
public class ActionCommand {

    /**
     * Current action command under construction. String of 0s and 1s. 0 means down, 1 means up volume-key-press.
     */
    private String command = "";
    private Handler handler = new Handler();

    private Context context;
    private Actions actions;

    ActionCommand(Context context, Actions actions) {
        this.context = context;
        this.actions = actions;
    }

    /**
     * Adds a command-bit to the command. Executes command if no other bit added before multi-press-time.
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
        Map<String, Action> mappings = Utils.getMappings(context, actions);
        Action action = mappings.get(command);
        if (action == null) {
            Log.i("<ME>", "Non-mapped command: " + command);
        }
        else {
            Log.i("<ME>", String.format("Execute %s -> %s", command, action.getName()));
            //action.run();
        }

        command = "";
    }

    /**
     * Discards command and cancels any callback.
     */
    void cancel() {
        command = "";
        handler.removeCallbacksAndMessages(null);
    }

    int getLength() {
        return command.length();
    }
}
