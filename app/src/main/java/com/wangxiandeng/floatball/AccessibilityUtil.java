package com.wangxiandeng.floatball;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.provider.Settings;

/**
 * Created by xingzhu on 2016/11/25.
 */

public class AccessibilityUtil {
    public static void doBack(AccessibilityService service) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    public static void doPullDown(AccessibilityService service) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
    }

    public static void doPullUp(AccessibilityService service) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    public static void doLeftOrRight(AccessibilityService service) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }

    public static boolean isAccessibilitySettingsOn(Context context) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null) {
                return services.toLowerCase().contains(context.getPackageName().toLowerCase());
            }
        }

        return false;
    }



}
