package com.wuch.coze.memory;

public interface DataMemory {
    void add(String key, String value);

    void add(String key, String hashKey, String value);

    String get(String key);

    String get(String key, String hashKey);

    void clear(String key);
}
