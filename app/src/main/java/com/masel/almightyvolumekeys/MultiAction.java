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
        if (pickedAction == null) return "Undetermined multi-action";
        else return pickedAction.getName();
    }

    @Override
    void run(MyContext myContext) throws ExecutionException {
        if (pickedAction == null) return;
        else pickedAction.run(myContext);
    }

    @Override
    NotifyOrder getNotifyOrder() {
        if (pickedAction == null) return NotifyOrder.ANY;
        else return pickedAction.getNotifyOrder();
    }

    @Override
    Notifier.VibrationPattern getVibrationPattern() {
        if (pickedAction == null) return Notifier.VibrationPattern.ON;
        else return pickedAction.getVibrationPattern();
    }

    @Override
    String[] getNeededPermissions(Context context) {
        return new String[]{};
    }
}
