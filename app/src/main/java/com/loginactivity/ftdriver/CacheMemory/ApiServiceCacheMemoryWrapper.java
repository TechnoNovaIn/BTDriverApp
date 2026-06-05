package com.loginactivity.ftdriver.CacheMemory;

import android.content.Context;

import androidx.annotation.NonNull;

import com.loginactivity.ftdriver.ApiService.ApiService;
import com.loginactivity.ftdriver.model.BTVehicle;
import com.loginactivity.ftdriver.model.Driver;
import com.loginactivity.ftdriver.model.Unload;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiServiceCacheMemoryWrapper {
    private ApiService apiService;
    private Context context;
    private final CacheMemoryManager cacheMemoryManager;

    private String s3AccessKey;
    private String s3SecretKey;


    public ApiServiceCacheMemoryWrapper(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
        this.cacheMemoryManager = CacheMemoryManager.getInstance();
    }


    public void getDriverNamesDetails(Callback<List<Driver>> callback) {
        String cacheKey = "DriverNames";
        List<Driver> cachedData = cacheMemoryManager.getData(cacheKey, List.class);

        if (cachedData != null) {
            callback.onResponse(null, Response.success(cachedData));
        } else {
            apiService.getDrivers().enqueue(new Callback<List<Driver>>() {
                @Override
                public void onResponse(@NonNull Call<List<Driver>> call, @NonNull Response<List<Driver>> response) {
                    if (response.isSuccessful()) {
                        List<Driver> data = response.body();
                        cacheMemoryManager.saveData(cacheKey, data);
                        callback.onResponse(call, Response.success(data));
                    } else {
                        callback.onFailure(call, new Throwable("DriverNumbers - API response Failed"));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Driver>> call, @NonNull Throwable t) {
                    callback.onFailure(call, t);
                }
            });
        }
    }

    public void getBTVehicleNumbersDetails(Callback<List<BTVehicle>> callback) {
        String cacheKey = "BTVehicleNumbers";
        List<BTVehicle> cachedData = cacheMemoryManager.getData(cacheKey, List.class);

        if (cachedData != null) {
            callback.onResponse(null, Response.success(cachedData));
        } else {
            apiService.getBTVehicles().enqueue(new Callback<List<BTVehicle>>() {
                @Override
                public void onResponse(Call<List<BTVehicle>> call, Response<List<BTVehicle>> response) {
                    if (response.isSuccessful()) {
                        List<BTVehicle> data = response.body();
                        cacheMemoryManager.saveData(cacheKey, data);
                        callback.onResponse(call, Response.success(data));
                    } else {
                        callback.onFailure(call, new Throwable("BTVehicleNumbers - API response Failed"));
                    }
                }
                @Override
                public void onFailure(Call<List<BTVehicle>> call, Throwable t) {
                    callback.onFailure(call, t);
                }
            });
        }
    }

    public void getAmazonAccessKey(Callback<String> callback) {
        if (s3AccessKey != null) {
            // Simulate successful response with cached key
            callback.onResponse(null, Response.success(s3AccessKey));
            return;
        }

        apiService.getAmazonS3AccessKey().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        s3AccessKey = response.body().string(); // convert raw body to plain string
                        // Now call the user's callback with string value
                        callback.onResponse(null, Response.success(s3AccessKey));
                    } catch (IOException e) {
                        // IO error while reading string from body
                        callback.onFailure(null, e);
                    }
                } else {
                    // Non-successful HTTP response
                    callback.onResponse(null, Response.success(null));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }



    public void getAmazonSecretKey(Callback<String> callback) {
        if (s3SecretKey != null) {
            // Return cached value if present
            callback.onResponse(null, Response.success(s3SecretKey));
            return;
        }

        // Make API call to fetch the secret key
        apiService.getAmazonS3SecretKey().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        s3SecretKey = response.body().string(); // Convert raw body to string
                        callback.onResponse(null, Response.success(s3SecretKey));
                    } catch (IOException e) {
                        callback.onFailure(null, e);
                    }
                } else {
                    callback.onResponse(null, Response.success(null));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }

    public void getUnloadLocation(Callback<List<Unload>> callback){
        String cacheKey = "UnloadLocations";
        List<Unload> cachedData = cacheMemoryManager.getData(cacheKey, List.class);

        if(cachedData != null){
            callback.onResponse(null, Response.success(cachedData));
        }else{
            apiService.getUnload().enqueue(new Callback<List<Unload>>() {
                @Override
                public void onResponse(Call<List<Unload>> call, Response<List<Unload>> response) {
                    if (response.isSuccessful()) {
                        List<Unload> data = response.body();
                        cacheMemoryManager.saveData(cacheKey, data);
                        callback.onResponse(call, Response.success(data));
                    } else {
                        callback.onFailure(call, new Throwable("UnloadLocation - API response failed"));
                    }
                }

                @Override
                public void onFailure(Call<List<Unload>> call, Throwable t) {
                    callback.onFailure(call, t);
                }
            });
        }

    }
}
