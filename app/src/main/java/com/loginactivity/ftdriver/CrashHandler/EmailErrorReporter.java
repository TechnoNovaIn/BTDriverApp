package com.loginactivity.ftdriver.CrashHandler;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

public class EmailErrorReporter {
    private static final String TAG = "EmailErrorReporter";
    private static Context context;
    public static String averageSpeed;

    public static void sendPostMethodErrorEmail(Context ctx, String subject, String functionName, String Username, String parameters, Integer responseCode, String responseMessage) {
        context = ctx;

        // Create a WorkRequest for the SpeedTestWorker
        WorkRequest speedTestRequest = new OneTimeWorkRequest.Builder(SpeedTestWorker.class)
                .addTag("speed_test")
                .build();

        WorkManager.getInstance(context).enqueue(speedTestRequest);

        // Observer to wait for the speed test result
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(speedTestRequest.getId())
                .observeForever(workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        // Retrieve speed from the output data of the worker
                        averageSpeed = workInfo.getOutputData().getString("speed");
                        Log.v(TAG, "Speed = " + averageSpeed);

                        // Now send the email with the retrieved speed
                        sendEmailReport(subject, functionName, Username, parameters, responseCode, responseMessage);
                    }
                });
    }

    public static void sendGetMethodErrorEmail(Context ctx, String subject, String functionName, String Username, Integer responseCode, String responseMessage) {
        context = ctx;

        // Create a WorkRequest for the SpeedTestWorker
        WorkRequest speedTestRequest = new OneTimeWorkRequest.Builder(SpeedTestWorker.class)
                .addTag("speed_test")
                .build();

        WorkManager.getInstance(context).enqueue(speedTestRequest);

        // Observer to wait for the speed test result
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(speedTestRequest.getId())
                .observeForever(workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        // Retrieve speed from the output data of the worker
                        averageSpeed = workInfo.getOutputData().getString("speed");
                        Log.v(TAG, "Speed = " + averageSpeed);

                        // Now send the email with the retrieved speed
                        sendEmailReport(subject, functionName, Username, null, responseCode, responseMessage);
                    }
                });
    }

    public static void sendInTangleMethodErrorEmail(Context ctx, String subject, String functionName, String Username, String parameters, Integer responseCode, String responseMessage) {
        context = ctx;
        sendEmailReport(subject, functionName, Username, parameters, responseCode, responseMessage);
    }

    private static void sendEmailReport(String subject, String functionName, String Username, String parameters, Integer responseCode, String responseMessage) {

        String deviceModel = Build.MODEL;
        String deviceBrand = Build.BRAND;
        String deviceManufacturer = Build.MANUFACTURER;
        String deviceOSVersion = Build.VERSION.RELEASE;

        StringBuilder body = new StringBuilder("Login User Name : " + Username + "\n\n");
        body.append("Method Name : ").append(functionName).append("\n\n");
        if (parameters != null) {
            body.append("Parameters : ").append(parameters).append("\n\n");
        }
        if (responseCode != null) {
            body.append("Response Code: ").append(responseCode).append("\n");
        }
        body.append("Response Message: ").append(responseMessage).append("\n\n");
        body.append("Please review the details and take appropriate action.\n\n");

        String network = getNetworkSpeed();
        String networkInfo =  "Network Information:\n" +"Network Type: " + network + "\n" + "Network Speed: " + averageSpeed + "\n\n";
        body.append(networkInfo);

        String deviceInfo = "Device Information:\n" +
                "Model: " + deviceModel + "\n" +
                "Brand: " + deviceBrand + "\n" +
                "Manufacturer: " + deviceManufacturer + "\n" +
                "OS Version: " + deviceOSVersion + "\n\n";
        body.append(deviceInfo);

        SendEmailSSL emailSender = new SendEmailSSL();
        emailSender.sendEmail(subject, body.toString());

        if (context != null) {
            Log.e(TAG, "Error : " + "API Error report sent");
        }
    }

    private static String getNetworkSpeed() {
        if (context == null) {
            return "Context is null";
        }

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

    private static String getMobileNetworkType(int networkType) {
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
