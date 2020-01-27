package com.masel.almightyvolumekeys;

import android.content.Context;

class TaskerAction extends Action {

    private String actionName;
    private String taskName = null;

    /**
     * @param actionName 'Tasker task' for unset tasker task, else 'Tasker: task-name'
     */
    TaskerAction(String actionName) {
        this.actionName = actionName;
        if (actionName.matches("^Tasker: .*")){
            this.taskName = actionName.replaceFirst("Tasker: ", "");
        }
    }

    @Override
    String getName() {
        return actionName;
    }

    @Override
    void run(MyContext myContext) throws ExecutionException {
        if (taskName == null) {
            throw new ExecutionException("Tasker error: No task specified");
        }

        switch (TaskerIntent.testStatus(myContext.context)) {
            case OK:
                myContext.context.sendBroadcast(new TaskerIntent(taskName));
                break;
            case NotInstalled: throw new ExecutionException("Tasker not installed");
            case NoPermission: throw new ExecutionException("Tasker error: Reinstall this app");
            case NotEnabled: throw new ExecutionException("Tasker error: Tasker is disabled");
            case AccessBlocked: throw new ExecutionException("Tasker error: Enable Tasker external access");
            case NoReceiver: throw new ExecutionException("Tasker error: Reinstall Tasker");
        }
    }

    @Override
    Notifier.VibrationPattern getVibrationPattern() {
        return Notifier.VibrationPattern.SILENT;
    }
}
