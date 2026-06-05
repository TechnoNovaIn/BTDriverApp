package com.loginactivity.ftdriver.CacheMemory;

import android.util.Log;

import java.util.HashMap;

public class CacheMemoryManager {

    private static final String TAG = "CacheMemoryManager";
    private static CacheMemoryManager instance;
    private HashMap<String, Object> cache;

    public CacheMemoryManager() {
        cache = new HashMap<>();
    }

    public static synchronized CacheMemoryManager getInstance() {
        if (instance == null) {
            instance = new CacheMemoryManager();
        }
        return instance;
    }

    public void saveData(String key, Object data) {
        Log.v(TAG, "SaveData key: " + key);
        cache.put(key, data);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Log.v(TAG, "GetData key: " + key);
        Object data = cache.get(key);
        if (data != null && type.isInstance(data)) {
            return type.cast(data);
        }
        return null;
    }

    public void clearCache() {
        cache.clear();
    }
}
