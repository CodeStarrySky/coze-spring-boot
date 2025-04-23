package com.wuch.coze.memory;

public interface DataMemory {
    void add(String key, String value);

    String get(String key);

    void clear(String key);

}
