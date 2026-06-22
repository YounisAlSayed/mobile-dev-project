package com.example.recyclerviewwebservice.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ProductWindowPolicyTest {
    @Test
    public void keepsDataWhenWindowDoesNotExceedCapacity() {
        assertEquals(0, ProductWindowPolicy.calculateOverflowCount(100, 50));
        assertEquals(0, ProductWindowPolicy.calculateOverflowCount(140, 10));
    }

    @Test
    public void removesOnlyTheExactOverflow() {
        assertEquals(10, ProductWindowPolicy.calculateOverflowCount(150, 10));
        assertEquals(25, ProductWindowPolicy.calculateOverflowCount(150, 25));
        assertEquals(50, ProductWindowPolicy.calculateOverflowCount(150, 50));
        assertEquals(50, ProductWindowPolicy.calculateOverflowCount(100, 100));
    }

    @Test
    public void removesTheExactOverflowForLargeInsertions() {
        assertEquals(250, ProductWindowPolicy.calculateOverflowCount(150, 250));
    }

    @Test
    public void rejectsInvalidCounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ProductWindowPolicy.calculateOverflowCount(-1, 10)
        );
    }
}
