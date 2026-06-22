package com.example.recyclerviewwebservice.model;

public final class ProductWindowPolicy {
    public static final int MAX_ITEMS_IN_MEMORY = 150;

    private ProductWindowPolicy() {
    }

    public static int calculateOverflowCount(int currentItemCount, int incomingItemCount) {
        if (currentItemCount < 0 || incomingItemCount < 0) {
            throw new IllegalArgumentException("Item counts cannot be negative.");
        }

        return Math.max(0, currentItemCount + incomingItemCount - MAX_ITEMS_IN_MEMORY);
    }
}
