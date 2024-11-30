package com.sky.filter.bloom;

import com.sky.filter.hash.HashStrategy;
import java.util.BitSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleBloomFilter implements BloomFilter {
    private final BitSet bitSet;
    private final int size;
    private final HashStrategy hashStrategy;

    public SimpleBloomFilter(int size, HashStrategy hashStrategy) {
        this.size = size;
        this.bitSet = new BitSet(size);
        this.hashStrategy = hashStrategy;
        log.info("创建布隆过滤器, size: {}", size);
    }

    @Override
    public void add(String key) {
        int hash = hashStrategy.hash(key);
        bitSet.set(Math.abs(hash % size));
        log.debug("添加key: {}, hash: {}", key, hash);
    }

    @Override
    public boolean contains(String key) {
        int hash = hashStrategy.hash(key);
        boolean result = bitSet.get(Math.abs(hash % size));
        log.debug("检查key: {}, hash: {}, 结果: {}", key, hash, result);
        return result;
    }
}