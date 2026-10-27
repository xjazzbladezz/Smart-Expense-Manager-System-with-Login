package com.smartexpense.ds;

import java.util.LinkedHashMap;
import java.util.Map;

/** Least-recently-used cache (access-ordered LinkedHashMap) for analytics results. */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity;

    public LRUCache(int capacity) {
        super(16, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
