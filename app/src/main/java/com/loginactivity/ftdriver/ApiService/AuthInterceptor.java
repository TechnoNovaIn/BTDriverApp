package com.loginactivity.ftdriver.ApiService;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private Context context;

    public AuthInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        SharedPreferences pref =
                context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        String token = pref.getString("Token", "");

        Request.Builder builder = chain.request().newBuilder();

        if (!token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
            Log.d("JWT", "Bearer " + token);
        }

        return chain.proceed(builder.build());
    }
}