package com.abosultan.darbaktools;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class RemoteAccessibilityService extends AccessibilityService {
    private static volatile RemoteAccessibilityService instance;

    public static boolean isReady() {
        return instance != null;
    }

    public static boolean tap(float x, float y) {
        RemoteAccessibilityService s = instance;
        if (s == null) return false;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 80));
        return s.dispatchGesture(builder.build(), null, null);
    }

    public static boolean longPress(float x, float y) {
        RemoteAccessibilityService s = instance;
        if (s == null) return false;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 700));
        return s.dispatchGesture(builder.build(), null, null);
    }

    public static boolean swipe(float x1, float y1, float x2, float y2, long duration) {
        RemoteAccessibilityService s = instance;
        if (s == null) return false;
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        long safeDuration = Math.max(120, Math.min(1200, duration));
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, safeDuration));
        return s.dispatchGesture(builder.build(), null, null);
    }

    public static boolean global(String action) {
        RemoteAccessibilityService s = instance;
        if (s == null || action == null) return false;
        if ("back".equals(action)) return s.performGlobalAction(GLOBAL_ACTION_BACK);
        if ("home".equals(action)) return s.performGlobalAction(GLOBAL_ACTION_HOME);
        if ("recents".equals(action)) return s.performGlobalAction(GLOBAL_ACTION_RECENTS);
        return false;
    }

    public static boolean setFocusedText(String text) {
        RemoteAccessibilityService s = instance;
        if (s == null) return false;
        AccessibilityNodeInfo root = s.getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (target == null) return false;
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text == null ? "" : text);
        boolean ok = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        target.recycle();
        root.recycle();
        return ok;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No continuous event processing is required. The service is used for gestures and global actions.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
