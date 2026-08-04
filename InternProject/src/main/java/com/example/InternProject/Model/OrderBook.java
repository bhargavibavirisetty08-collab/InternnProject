package com.example.InternProject.Model;

import java.util.*;

public class OrderBook {
    private final TreeMap<Double, ArrayDeque<Order>> buyLevels =
            new TreeMap<>(Collections.reverseOrder());

    // SELL → lowest price first
    private final TreeMap<Double, ArrayDeque<Order>> sellLevels =
            new TreeMap<>();

    public TreeMap<Double, ArrayDeque<Order>> getBuyLevels() {
        return buyLevels;
    }

    public TreeMap<Double, ArrayDeque<Order>> getSellLevels() {
        return sellLevels;
    }
}
