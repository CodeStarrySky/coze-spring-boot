package com.wuch.coze.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultDataMemory implements DataMemory{

    Map<String, String> history = new ConcurrentHashMap<>();

    @Override
    public void add(String key, String value) {
        this.history.put(key, value);
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
