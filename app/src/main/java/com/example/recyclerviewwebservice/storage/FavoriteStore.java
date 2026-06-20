package com.example.recyclerviewwebservice.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Persists favorite product IDs across reloads and app restarts. */
public class FavoriteStore {
    private static final String PREFERENCES_NAME = "product_favorites";
    private static final String FAVORITE_IDS_KEY = "favorite_ids";

    private final SharedPreferences preferences;
    private final Set<String> favoriteIds;

    public FavoriteStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        favoriteIds = new HashSet<>(preferences.getStringSet(
                FAVORITE_IDS_KEY,
                Collections.emptySet()
        ));
    }

    public synchronized boolean isFavorite(String productId) {
        return favoriteIds.contains(productId);
    }

    public synchronized boolean toggle(String productId) {
        boolean nowFavorite;
        if (favoriteIds.contains(productId)) {
            favoriteIds.remove(productId);
            nowFavorite = false;
        } else {
            favoriteIds.add(productId);
            nowFavorite = true;
        }
        preferences.edit()
                .putStringSet(FAVORITE_IDS_KEY, new HashSet<>(favoriteIds))
                .apply();
        return nowFavorite;
    }
}
