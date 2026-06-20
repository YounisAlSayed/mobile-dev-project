package com.example.recyclerviewwebservice.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProductTest {
    private Product createProduct(String id) {
        return new Product(
                id,
                "The Picture of Dorian Gray",
                "Oscar Wilde",
                "https://covers.openlibrary.org/b/id/14314858-M.jpg",
                2499,
                1890,
                3012
        );
    }

    @Test
    public void constructorExposesProductCardFields() {
        Product product = createProduct("/works/OL8193416W");

        assertEquals("/works/OL8193416W", product.getId());
        assertEquals("The Picture of Dorian Gray", product.getTitle());
        assertEquals("Oscar Wilde", product.getSubtitle());
        assertTrue(product.getImageUrl().startsWith("https://"));
        assertEquals(2499, product.getPriceCents());
        assertEquals(1890, product.getFirstPublishYear());
        assertEquals(3012, product.getEditionCount());
    }

    @Test
    public void favoriteStateCanChangeWithoutChangingProductIdentity() {
        Product product = createProduct("/works/OL8193416W");
        Product same = createProduct("/works/OL8193416W");

        assertFalse(product.isFavorite());
        product.setFavorite(true);

        assertTrue(product.isFavorite());
        assertEquals(product, same);
        assertEquals(product.getStableId(), same.getStableId());
    }

    @Test
    public void differentSourceIdsHaveDifferentStableIds() {
        Product first = createProduct("/works/OL1W");
        Product second = createProduct("/works/OL2W");

        assertNotEquals(first, second);
        assertNotEquals(first.getStableId(), second.getStableId());
    }

    @Test
    public void demoPricesAreStableAndInsideConfiguredRange() {
        int first = DemoPriceCalculator.forProductId("/works/OL8193416W");
        int second = DemoPriceCalculator.forProductId("/works/OL8193416W");

        assertEquals(first, second);
        assertTrue(first >= 799);
        assertTrue(first <= 7999);
    }
}
