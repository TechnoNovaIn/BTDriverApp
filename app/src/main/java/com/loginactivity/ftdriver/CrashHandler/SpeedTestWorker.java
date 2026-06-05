package com.loginactivity.ftdriver.CrashHandler;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpeedTestWorker extends Worker {
    private static final String TAG = "SpeedTestWorker";
    private static final String TEST_URL = "https://technonova.in/";

    public SpeedTestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        long startTime = System.currentTimeMillis();
        int fileSizeInBytes = 0;

        try {
            // Use OkHttp to fetch data
            OkHttpClient client = getUnsafeOkHttpClient();
            Request request = new Request.Builder().url(TEST_URL).build();

            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                byte[] data = response.body().bytes();
                fileSizeInBytes = data.length;
            }
            response.close();
        } catch (IOException e) {
            Log.e(TAG, "Error during speed test: " + e.getMessage());
            return ListenableWorker.Result.failure();
        }

        long endTime = System.currentTimeMillis();
        long timeTakenInMillis = endTime - startTime;

        // Convert time from milliseconds to seconds
        double timeTakenInSeconds = timeTakenInMillis / 1000.0;

        // Calculate speed in KBps (Kilobytes per second)
        double speedKBps = fileSizeInBytes / timeTakenInSeconds / 1024.0; // Convert from bytes to KB

        // Convert KBps to Mbps (Megabits per second)
        double speedMbps = speedKBps * 8 / 1024.0; // 1 KBps = 8 Kbps, 1 Mbps = 1024 Kbps

        // Log the speeds with better formatting
        Log.d(TAG, String.format("Internet Speed: %.2f KBps", speedKBps));
        Log.d(TAG, String.format("Internet Speed: %.2f Mbps", speedMbps));


        // Store the speed in the output data
        Data outputData = new Data.Builder()
                .putString("speed", String.format("%.2f KB/s", speedKBps))
                .build();

        // Return success
        return ListenableWorker.Result.success(outputData);
    }


    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Build an OkHttpClient with the custom SSLSocketFactory
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
