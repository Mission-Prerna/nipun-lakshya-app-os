package org.odk.collect.android.utilities;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.odk.collect.android.application.Collect1;


public class ToastUtils {

    private ToastUtils() {

    }

    public static void showShortToast(String message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    public static void showShortToast(int messageResource) {
        showToast(messageResource, Toast.LENGTH_SHORT);
    }

    public static void showLongToast(String message) {
        showToast(message, Toast.LENGTH_LONG);
    }

    public static void showLongToast(int messageResource) {
        showToast(messageResource, Toast.LENGTH_LONG);
    }

    private static void showToast(String message, int duration) {
        Toast.makeText(Collect1.getInstance().getAppContext(), message, duration).show();
    }

    private static void showToast(int messageResource, int duration) {
        Toast.makeText(Collect1.getInstance().getAppContext(), Collect1.getInstance().getAppContext().getResources().getString(messageResource), duration).show();
    }

    public static void showShortToastInMiddle(int messageResource) {
        showToastInMiddle(Collect1.getInstance().getAppContext().getResources().getString(messageResource), Toast.LENGTH_SHORT);
    }

    public static void showShortToastInMiddle(String message) {
        showToastInMiddle(message, Toast.LENGTH_SHORT);
    }

    public static void showLongToastInMiddle(int messageResource) {
        showToastInMiddle(Collect1.getInstance().getAppContext().getResources().getString(messageResource), Toast.LENGTH_LONG);
    }

    private static void showToastInMiddle(String message, int duration) {
        Toast toast = Toast.makeText(Collect1.getInstance().getAppContext(), message, duration);
        try {
            ViewGroup group = (ViewGroup) toast.getView();
            TextView messageTextView = (TextView) group.getChildAt(0);
            messageTextView.setTextSize(21);
            messageTextView.setGravity(Gravity.CENTER);
        } catch (Exception ignored) {
            // ignored
        }

        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
