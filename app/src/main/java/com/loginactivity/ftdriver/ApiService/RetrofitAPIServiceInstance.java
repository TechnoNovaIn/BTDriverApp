package com.loginactivity.ftdriver.ApiService;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitAPIServiceInstance {
    private static Retrofit retrofit;
    private static final String BASE_URL = "https://external.balajitransports.in/api/";

    public static ApiService getApiService() throws Exception {
        if (retrofit == null) {
//            retrofit = createRetrofit();
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(500); // total
            dispatcher.setMaxRequestsPerHost(200);

//            OkHttpClient client = new OkHttpClient.Builder()
//                    .dispatcher(dispatcher)
//                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.MINUTES)
                    .readTimeout(10, TimeUnit.MINUTES)
                    .writeTimeout(10, TimeUnit.MINUTES)
                    .dispatcher(dispatcher)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // Replace with your actual URL
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    private static Retrofit createRetrofit() throws Exception {
        OkHttpClient okHttpClient = createSecureOkHttpClient();
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    private static OkHttpClient createSecureOkHttpClient() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts(), new SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts()[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectTimeout(30, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.MINUTES)
                .build();
    }

    private static TrustManager[] trustAllCerts() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }
}
