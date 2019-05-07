package com.example.per6.hydrationapp.utils;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.view.Window;
import android.view.WindowManager;

public class DialogUtils {

    // Prevent dialog dismiss when orientation changes
    // http://stackoverflow.com/questions/7557265/prevent-dialog-dismissal-on-screen-rotation-in-android
    public static void keepDialogOnOrientationChanges(@NonNull Dialog dialog) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            if (attributes != null) {
                lp.copyFrom(attributes);
                lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                dialog.getWindow().setAttributes(lp);
            }
        }
    }

}