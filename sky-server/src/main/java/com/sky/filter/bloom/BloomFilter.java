package com.sky.filter.bloom;

public interface BloomFilter {
    void add(String key);

    boolean contains(String key);
}
