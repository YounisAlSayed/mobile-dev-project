package com.example.recyclerviewwebservice.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.recyclerviewwebservice.R;
import com.example.recyclerviewwebservice.model.Product;
import com.example.recyclerviewwebservice.model.ProductWindowPolicy;
import com.example.recyclerviewwebservice.storage.FavoriteStore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Binds complex e-commerce cards to the official AndroidX RecyclerView. */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private static final String PAYLOAD_FAVORITE = "favorite";

    private final List<Product> products = new ArrayList<>();
    private final FavoriteStore favoriteStore;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public ProductAdapter(FavoriteStore favoriteStore) {
        this.favoriteStore = favoriteStore;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.titleText.setText(product.getTitle());
        holder.subtitleText.setText(product.getSubtitle());
        holder.metadataText.setText(buildMetadata(product));
        holder.priceText.setText(currencyFormat.format(product.getPriceCents() / 100.0));

        Glide.with(holder.productImage)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(holder.productImage);

        bindFavorite(holder, product);
        holder.favoriteButton.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }
            Product currentProduct = products.get(currentPosition);
            currentProduct.setFavorite(favoriteStore.toggle(currentProduct.getId()));
            notifyItemChanged(currentPosition, PAYLOAD_FAVORITE);
        });
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position,
            @NonNull List<Object> payloads
    ) {
        if (payloads.contains(PAYLOAD_FAVORITE)) {
            bindFavorite(holder, products.get(position));
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private void bindFavorite(ProductViewHolder holder, Product product) {
        boolean favorite = product.isFavorite();
        holder.favoriteButton.setImageResource(
                favorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border
        );
        holder.favoriteButton.setContentDescription(holder.itemView.getContext().getString(
                favorite ? R.string.remove_from_favorites : R.string.add_to_favorites,
                product.getTitle()
        ));
    }

    private String buildMetadata(Product product) {
        if (product.getFirstPublishYear() > 0 && product.getEditionCount() > 0) {
            return String.format(
                    Locale.getDefault(),
                    "PUBLISHED %d  •  %,d EDITIONS",
                    product.getFirstPublishYear(),
                    product.getEditionCount()
            );
        }
        if (product.getFirstPublishYear() > 0) {
            return String.format(Locale.getDefault(), "PUBLISHED %d", product.getFirstPublishYear());
        }
        if (product.getEditionCount() > 0) {
            return String.format(Locale.getDefault(), "%,d EDITIONS", product.getEditionCount());
        }
        return "CATALOG PRODUCT";
    }

    @Override
    public void onViewRecycled(@NonNull ProductViewHolder holder) {
        Glide.with(holder.productImage).clear(holder.productImage);
        holder.favoriteButton.setOnClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public long getItemId(int position) {
        return products.get(position).getStableId();
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    /** Appends newer products and removes only the exact overflow from the front. */
    public int appendProducts(List<Product> newProducts) {
        int removalCount = ProductWindowPolicy.calculateOverflowCount(
                products.size(),
                newProducts.size()
        );
        restoreFavoriteState(newProducts);

        if (removalCount > 0) {
            products.subList(0, removalCount).clear();
            notifyItemRangeRemoved(0, removalCount);
        }

        int insertionStart = products.size();
        products.addAll(newProducts);
        notifyItemRangeInserted(insertionStart, newProducts.size());
        return removalCount;
    }

    /** Prepends older products and removes only the exact overflow from the end. */
    public int prependProducts(List<Product> newProducts) {
        int removalCount = ProductWindowPolicy.calculateOverflowCount(
                products.size(),
                newProducts.size()
        );
        restoreFavoriteState(newProducts);

        if (removalCount > 0) {
            int removalStart = products.size() - removalCount;
            products.subList(removalStart, products.size()).clear();
            notifyItemRangeRemoved(removalStart, removalCount);
        }

        products.addAll(0, newProducts);
        notifyItemRangeInserted(0, newProducts.size());
        return removalCount;
    }

    private void restoreFavoriteState(List<Product> newProducts) {
        for (Product product : newProducts) {
            product.setFavorite(favoriteStore.isFavorite(product.getId()));
        }
    }

    public void clear() {
        int oldSize = products.size();
        products.clear();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ImageView productImage;
        final TextView titleText;
        final TextView subtitleText;
        final TextView metadataText;
        final TextView priceText;
        final ImageButton favoriteButton;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            titleText = itemView.findViewById(R.id.titleText);
            subtitleText = itemView.findViewById(R.id.subtitleText);
            metadataText = itemView.findViewById(R.id.metadataText);
            priceText = itemView.findViewById(R.id.priceText);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }
}
