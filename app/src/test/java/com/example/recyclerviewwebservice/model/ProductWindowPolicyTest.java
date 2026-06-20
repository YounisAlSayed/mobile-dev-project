package com.example.recyclerviewwebservice.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ProductWindowPolicyTest {
    @Test
    public void keepsDataWhenWindowDoesNotExceedCapacity() {
        assertEquals(0, ProductWindowPolicy.calculateRemovalCount(100, 50, 50));
        assertEquals(0, ProductWindowPolicy.calculateRemovalCount(140, 10, 10));
    }

    @Test
    public void removesOneOldestCompletePackWhenCapacityIsExceeded() {
        assertEquals(10, ProductWindowPolicy.calculateRemovalCount(150, 10, 10));
        assertEquals(25, ProductWindowPolicy.calculateRemovalCount(150, 25, 25));
        assertEquals(50, ProductWindowPolicy.calculateRemovalCount(150, 50, 50));
        assertEquals(100, ProductWindowPolicy.calculateRemovalCount(100, 100, 100));
    }

    @Test
    public void removesMultipleCompletePacksForLargeInsertions() {
        assertEquals(250, ProductWindowPolicy.calculateRemovalCount(150, 250, 50));
    }

    @Test
    public void rejectsInvalidCounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ProductWindowPolicy.calculateRemovalCount(-1, 10, 10)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProductWindowPolicy.calculateRemovalCount(10, 10, 0)
        );
    }
}
