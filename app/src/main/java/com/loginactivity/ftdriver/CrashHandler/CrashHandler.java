package com.loginactivity.ftdriver.CrashHandler;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

public class CrashHandler implements Thread.UncaughtExceptionHandler{

    private final Context context;
    private static final String TAG = "CrashHandler";

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            sendCrashReport(throwable);
        } catch (Exception e) {
            Log.e(TAG, "Error sending crash report", e);
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    private void sendCrashReport(Throwable throwable) {

        Log.v(TAG, "Sending crash report...");
        String crashReport = Log.getStackTraceString(throwable);

        SharedPreferences sharedPreferences = context.getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        String UserName = sharedPreferences.getString("Name", "");

        String deviceModel = Build.MODEL;
        String deviceBrand = Build.BRAND;
        String deviceManufacturer = Build.MANUFACTURER;
        String deviceOSVersion = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;

        String network = getNetworkSpeed();

        String deviceInfo = "\nDevice Information:\n" +
                "Model: " + deviceModel + "\n" +
                "Brand: " + deviceBrand + "\n" +
                "Manufacturer: " + deviceManufacturer + "\n" +
                "osVersion: " + deviceOSVersion + "\n" +
                "SDK Version: " + sdkVersion + "\n" +
                "Network Type: " + network + "\n";

        String userInfo = "\nUser Name: " + UserName + "\n";

        String fullCrashReport = crashReport + userInfo + deviceInfo;
        Log.e(TAG, "Full Crash Report: " + fullCrashReport);

        SendEmailSSL emailSender = new SendEmailSSL();
        emailSender.sendEmail("Crash Report", fullCrashReport);
    }

    private String getNetworkSpeed() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities nc = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (nc != null) {
                    if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return "WiFi - Fast";
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                            return "Cellular - Unlimited Data";
                        } else {
                            return "Cellular - Metered Data";
                        }
                    }
                }
            } else {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        return "WiFi - Fast";
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        return "Cellular - " + getMobileNetworkType(activeNetwork.getSubtype());
                    }
                }
            }
        }
        return "Unknown";
    }

    private String getMobileNetworkType(int networkType) {
        switch (networkType) {
            case 13:
                return "4G/LTE";
            case 15:
                return "HSPA+";
            case 3:
                return "3G";
            case 1:
                return "GPRS";
            case 20:
                return "5G";
            default:
                return "Unknown";
        }
    }
}
