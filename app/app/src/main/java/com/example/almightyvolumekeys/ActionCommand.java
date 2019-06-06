package com.example.almightyvolumekeys;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Map;

/**
 * Builds a command and executes it.
 */
public class ActionCommand {

    /**
     * Current action command under construction. String of 0s and 1s. 0 means down, 1 means up volume-key-press.
     */
    private String command = "";
    private Handler handler = new Handler();

    private Context context;
    private Actions actions;

    public ActionCommand(Context context, Actions actions) {
        this.context = context;
        this.actions = actions;
    }

    /**
     * Adds a command-block to the command. Executes command if no other block added before multi-press-time.
     * @param up else down-command-block
     */
    public void addBlock(boolean up) {
        long MULTI_PRESS_MAX_TIME = 400;
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
        if (actions == null) {
            Log.i("<ME>", "Non-mapped command: " + command);
        }
        else {
            Log.i("<ME>", String.format("Execute %s -> %s", command, action.getName()));
            //action.run();
        }

        command = "";
    }
}
