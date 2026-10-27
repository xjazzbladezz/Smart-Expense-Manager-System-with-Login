package com.smartexpense.ds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Keeps the N largest items of a stream using a bounded min-heap: O(M log N) for M items.
 */
public class TopN<T> {

    private final int n;
    private final Comparator<T> comparator;
    private final PriorityQueue<T> heap;

    public TopN(int n, Comparator<T> comparator) {
        this.n = n;
        this.comparator = comparator;
        this.heap = new PriorityQueue<>(Math.max(1, n), comparator);
    }

    public void offer(T item) {
        if (heap.size() < n) {
            heap.add(item);
        } else if (comparator.compare(item, heap.peek()) > 0) {
            heap.poll();
            heap.add(item);
        }
    }

    /** Largest first. */
    public List<T> toSortedList() {
        List<T> out = new ArrayList<>(heap);
        out.sort(comparator.reversed());
        return out;
    }
}
