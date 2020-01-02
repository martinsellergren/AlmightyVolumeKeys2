package com.masel.almightyvolumekeys;

import android.content.Context;

/**
 * For creating toggles etc.
 * Note: Call pickAction() before anything else, except: neededPermissions and minApi accessible anytime.
 */
abstract class MultiAction extends Action {

    private Action pickedAction = null;

    void setAction(MyContext myContext) {
        this.pickedAction = pickAction(myContext);
    }

    abstract Action pickAction(MyContext myContext);

    @Override
    String getName() {
        return pickedAction.getName();
    }

    @Override
    void run(MyContext myContext) throws ExecutionException {
        pickedAction.run(myContext);
    }

    @Override
    String getDescription() {
        return pickedAction.getDescription();
    }

    @Override
    String[] getNotes() {
        return pickedAction.getNotes();
    }

    @Override
    NotifyOrder getNotifyOrder() {
        return pickedAction.getNotifyOrder();
    }

    @Override
    Notifier.VibrationPattern getVibrationPattern() {
        return pickedAction.getVibrationPattern();
    }

    @Override
    String[] getNeededPermissions(Context context) {
        return new String[]{};
    }
//
//    @Override
//    boolean isAvailable(Context context) {
//        return true;
//    }
}
