package com.dji.uilibrarydemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;

public class PermissionUtil {

    public static int REQUEST_ALL_DANGEROUS_PERMISSION = 0;
    public static int REQUEST_PERMISSION_PHONE_STATE = 1;

    private static String[] getDangerousPermissions() {
        return new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_CONFIGURATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
        };
    }

    public static void checkPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] arrayPermission = getDangerousPermissions();
            ArrayList<String> needGrads = new ArrayList<>();
            for (String permission : arrayPermission) {
                if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    needGrads.add(permission);
                }
            }
            ActivityCompat.requestPermissions(activity, needGrads.toArray(new String[0]), REQUEST_ALL_DANGEROUS_PERMISSION);
        }
    }

    public static boolean checkPhoneStatePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permission = Manifest.permission.READ_PHONE_STATE;
            if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{permission},
                        REQUEST_PERMISSION_PHONE_STATE
                );
            }
        }
        return false;
    }

}
