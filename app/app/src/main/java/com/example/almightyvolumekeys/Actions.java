package com.example.almightyvolumekeys;

import android.content.Context;

/**
 * Executes the actions.
 */
public class Actions {

    public interface Action {
        String getName();
        String getDescription();
        void run();
    }

    public class DefaultVolumeUp implements Action {
        @Override
        public String getName() {
            return "Increase default volume";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void run() {

        }
    }
}
