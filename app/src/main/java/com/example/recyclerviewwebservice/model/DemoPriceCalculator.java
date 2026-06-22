package com.example.recyclerviewwebservice.model;

public final class DemoPriceCalculator {
    private static final int MIN_PRICE_CENTS = 799;
    private static final int PRICE_RANGE_CENTS = 7_201;

    private DemoPriceCalculator() {
    }

    public static int forProductId(String productId) {
        long unsignedHash = productId.hashCode() & 0xffffffffL;
        return MIN_PRICE_CENTS + (int) (unsignedHash % PRICE_RANGE_CENTS);
    }
}
