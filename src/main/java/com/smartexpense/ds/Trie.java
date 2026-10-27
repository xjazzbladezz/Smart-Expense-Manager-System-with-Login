package com.smartexpense.ds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prefix tree used for instant, frequency-ranked auto-suggestions of transaction descriptions.
 * Insert = O(L), suggest = O(L + K log K) where L is the word length and K the matches.
 */
public class Trie {

    private static final class Node {
        final Map<Character, Node> next = new HashMap<>();
        int frequency;
        String display;
    }

    private final Node root = new Node();
    private int words;

    public void insert(String word) {
        if (word == null) return;
        String w = word.trim();
        if (w.isEmpty()) return;
        Node cur = root;
        for (char ch : w.toLowerCase().toCharArray()) {
            cur = cur.next.computeIfAbsent(ch, k -> new Node());
        }
        if (cur.frequency == 0) words++;
        cur.frequency++;
        cur.display = w;
    }

    /** Up to {@code limit} stored words starting with {@code prefix}, most frequent first. */
    public List<String> suggest(String prefix, int limit) {
        List<Node> hits = new ArrayList<>();
        if (prefix == null) return List.of();
        Node cur = root;
        for (char ch : prefix.trim().toLowerCase().toCharArray()) {
            cur = cur.next.get(ch);
            if (cur == null) return List.of();
        }
        collect(cur, hits);
        hits.sort(Comparator.<Node>comparingInt(n -> -n.frequency)
                .thenComparing(n -> n.display, String.CASE_INSENSITIVE_ORDER));
        List<String> out = new ArrayList<>();
        for (Node n : hits) {
            if (out.size() >= limit) break;
            out.add(n.display);
        }
        return out;
    }

    private void collect(Node n, List<Node> out) {
        if (n.frequency > 0) out.add(n);
        for (Node child : n.next.values()) collect(child, out);
    }

    public int size() {
        return words;
    }

    public void clear() {
        root.next.clear();
        words = 0;
    }
}
