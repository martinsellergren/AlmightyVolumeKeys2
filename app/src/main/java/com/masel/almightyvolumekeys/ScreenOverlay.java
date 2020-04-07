package com.masel.almightyvolumekeys;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.masel.rec_utils.RecUtils;

class ScreenOverlay {

    private WindowManager windowManager;

    ScreenOverlay(Context context) {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    private View overlayView = null;

    private void setupOverlayWindow(Context context) {
        overlayView = new View(context);
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type, flags, PixelFormat.TRANSLUCENT);
        topLeftParams.gravity = Gravity.LEFT | Gravity.TOP;
        topLeftParams.x = 0;
        topLeftParams.y = 0;
//        topLeftParams.width = 0;
//        topLeftParams.height = 0;
        windowManager.addView(overlayView, topLeftParams);
        overlayView.setVisibility(View.GONE);
    }

    void destroy() {
        actionHandler.removeCallbacksAndMessages(null);
        windowManager.removeView(overlayView);
    }

    private Handler actionHandler = new Handler();
    private long ACTION_DELAY = 750;

    void runAction(Context context, Runnable action) throws Action.ExecutionException {
        Runnable completeAction = () -> {
            overlayView.setVisibility(View.VISIBLE);
            actionHandler.postDelayed(() -> {
                action.run();
                overlayView.setVisibility(View.GONE);
            }, ACTION_DELAY);
        };

        if (overlayView == null) {
            setupOverlayWindow(context);
            overlayView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    overlayView.removeOnAttachStateChangeListener(this);
                    new Handler().postDelayed(completeAction::run, 1000);
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    overlayView.removeOnAttachStateChangeListener(this);
                }
            });
        }
        else {
            completeAction.run();
        }
    }
}
