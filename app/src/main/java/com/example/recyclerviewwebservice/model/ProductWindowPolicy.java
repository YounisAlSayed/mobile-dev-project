package com.example.recyclerviewwebservice.model;

/** Keeps the adapter's product-object window bounded while removing complete packs. */
public final class ProductWindowPolicy {
    public static final int MAX_ITEMS_IN_MEMORY = 150;

    private ProductWindowPolicy() {
    }

    public static int calculateRemovalCount(
            int currentItemCount,
            int incomingItemCount,
            int packSize
    ) {
        if (currentItemCount < 0 || incomingItemCount < 0 || packSize <= 0) {
            throw new IllegalArgumentException("Counts cannot be negative and pack size must be positive.");
        }

        int overflow = currentItemCount + incomingItemCount - MAX_ITEMS_IN_MEMORY;
        if (overflow <= 0) {
            return 0;
        }

        int completePacksToRemove = (overflow + packSize - 1) / packSize;
        return Math.min(
                currentItemCount + incomingItemCount,
                completePacksToRemove * packSize
        );
    }
}
