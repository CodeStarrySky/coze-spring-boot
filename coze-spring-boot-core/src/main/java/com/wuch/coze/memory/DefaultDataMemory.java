package com.wuch.coze.memory;

import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultDataMemory implements DataMemory{

    Map<String, String> history = new ConcurrentHashMap<>();

    Map<String, ConcurrentHashMap<String, String>> hashHistory =new ConcurrentHashMap<>();

    @Override
    public void add(String key, String value) {
        this.history.put(key, value);
    }


    @Override
    public void add(String key, String hashKey, String value) {
        hashHistory.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(hashKey, value);
    }

    @Override
    public String get(String key, String hashKey) {
        ConcurrentHashMap<String, String> map = hashHistory.get(key);
        if (map == null) {
            return null;
        }
        return map.get(hashKey);
    }

    @Override
    public String get(String key) {
        return history.get(key);
    }

    @Override
    public void clear(String key) {
        history.remove(key);
    }
}
