package com.sky.filter.hash;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class SimpleHashStrategy implements HashStrategy {
    @Override
    public int hash(String key) {
        return Hashing.murmur3_32_fixed()
                .hashString(key, StandardCharsets.UTF_8)
                .asInt();
    }
}