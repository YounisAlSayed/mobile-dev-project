package com.example.recyclerviewwebservice.model;

import java.util.Objects;

/** One e-commerce product derived from an Open Library work. */
public class Product {
    private final String id;
    private final String title;
    private final String subtitle;
    private final String imageUrl;
    private final int priceCents;
    private final int firstPublishYear;
    private final int editionCount;
    private boolean favorite;

    public Product(
            String id,
            String title,
            String subtitle,
            String imageUrl,
            int priceCents,
            int firstPublishYear,
            int editionCount
    ) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.priceCents = priceCents;
        this.firstPublishYear = firstPublishYear;
        this.editionCount = editionCount;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getFirstPublishYear() {
        return firstPublishYear;
    }

    public int getEditionCount() {
        return editionCount;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /** Creates a deterministic 64-bit RecyclerView ID from the source work ID. */
    public long getStableId() {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < id.length(); index++) {
            hash ^= id.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Product)) {
            return false;
        }
        Product product = (Product) object;
        return priceCents == product.priceCents
                && firstPublishYear == product.firstPublishYear
                && editionCount == product.editionCount
                && Objects.equals(id, product.id)
                && Objects.equals(title, product.title)
                && Objects.equals(subtitle, product.subtitle)
                && Objects.equals(imageUrl, product.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                title,
                subtitle,
                imageUrl,
                priceCents,
                firstPublishYear,
                editionCount
        );
    }
}
