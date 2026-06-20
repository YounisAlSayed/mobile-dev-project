package com.example.recyclerviewwebservice;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recyclerviewwebservice.model.Product;
import com.example.recyclerviewwebservice.network.OpenLibraryProductApi;
import com.example.recyclerviewwebservice.storage.FavoriteStore;
import com.example.recyclerviewwebservice.ui.ProductAdapter;

import java.text.NumberFormat;
import java.util.List;

/**
 * Main screen for a recycled e-commerce product feed backed by Open Library.
 */
public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int LOAD_MORE_THRESHOLD = 5;
    private static final String PAGE_SIZE_PREFERENCE = "page_size";

    private final OpenLibraryProductApi api = new OpenLibraryProductApi();
    private ProductAdapter adapter;

    private TextView statusText;
    private Button retryButton;
    private ProgressBar initialProgress;
    private ProgressBar loadMoreProgress;
    private RecyclerView recyclerView;

    private int pageSize;
    private int nextPage = 1;
    private int requestGeneration;
    private long totalAvailable;
    private boolean loading;
    private boolean reachedEnd;
    private boolean userScrollActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        retryButton = findViewById(R.id.retryButton);
        initialProgress = findViewById(R.id.initialProgress);
        loadMoreProgress = findViewById(R.id.loadMoreProgress);
        recyclerView = findViewById(R.id.productsRecyclerView);
        Spinner pageSizeSpinner = findViewById(R.id.pageSizeSpinner);

        adapter = new ProductAdapter(new FavoriteStore(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);

        pageSize = getPreferences(MODE_PRIVATE).getInt(
                PAGE_SIZE_PREFERENCE,
                DEFAULT_PAGE_SIZE
        );
        configurePageSizeSpinner(pageSizeSpinner);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int newState) {
                super.onScrollStateChanged(view, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    userScrollActive = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    userScrollActive = false;
                }
            }

            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                super.onScrolled(view, dx, dy);
                if (!userScrollActive || dy <= 0 || loading || reachedEnd) {
                    return;
                }

                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - LOAD_MORE_THRESHOLD) {
                    loadNextPage();
                }
            }
        });

        findViewById(R.id.reloadButton).setOnClickListener(view -> resetAndLoad());

        retryButton.setOnClickListener(view -> {
            retryButton.setVisibility(View.GONE);
            if (adapter.getItemCount() == 0) {
                resetAndLoad();
            } else {
                loadNextPage();
            }
        });

        loadNextPage();
    }

    private void configurePageSizeSpinner(Spinner spinner) {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.page_size_options,
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        int selectedIndex = 0;
        for (int index = 0; index < spinnerAdapter.getCount(); index++) {
            if (Integer.parseInt(spinnerAdapter.getItem(index).toString()) == pageSize) {
                selectedIndex = index;
                break;
            }
        }
        spinner.setSelection(selectedIndex, false);
        pageSize = Integer.parseInt(spinnerAdapter.getItem(selectedIndex).toString());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedPageSize = Integer.parseInt(
                        parent.getItemAtPosition(position).toString()
                );
                if (selectedPageSize == pageSize) {
                    return;
                }

                pageSize = selectedPageSize;
                getPreferences(MODE_PRIVATE)
                        .edit()
                        .putInt(PAGE_SIZE_PREFERENCE, pageSize)
                        .apply();
                resetAndLoad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep the existing pack size.
            }
        });
    }

    /** Clears every row and paging value before requesting a new first pack. */
    private void resetAndLoad() {
        requestGeneration++;
        adapter.clear();
        recyclerView.scrollToPosition(0);
        nextPage = 1;
        totalAvailable = 0L;
        reachedEnd = false;
        loading = false;
        userScrollActive = false;
        retryButton.setVisibility(View.GONE);
        loadMoreProgress.setVisibility(View.GONE);
        loadNextPage();
    }

    private void loadNextPage() {
        if (loading || reachedEnd) {
            return;
        }

        loading = true;
        retryButton.setVisibility(View.GONE);
        statusText.setText(getString(R.string.loading_pack, nextPage, pageSize));

        if (adapter.getItemCount() == 0) {
            initialProgress.setVisibility(View.VISIBLE);
        } else {
            loadMoreProgress.setVisibility(View.VISIBLE);
        }

        final int generation = requestGeneration;
        final int requestedPage = nextPage;
        final int requestedPageSize = pageSize;

        api.fetchProducts(requestedPage, requestedPageSize, new OpenLibraryProductApi.Callback() {
            @Override
            public void onRetry(int nextAttempt, int maximumAttempts, long delayMillis) {
                if (!isCurrentRequest(generation)) {
                    return;
                }

                long delaySeconds = Math.max(1L, delayMillis / 1_000L);
                statusText.setText(getString(
                        R.string.waiting_for_network,
                        nextAttempt,
                        maximumAttempts,
                        delaySeconds
                ));
            }

            @Override
            public void onSuccess(List<Product> products, long sourceTotal) {
                if (!isCurrentRequest(generation)) {
                    return;
                }

                loading = false;
                initialProgress.setVisibility(View.GONE);
                loadMoreProgress.setVisibility(View.GONE);
                totalAvailable = sourceTotal;
                adapter.addProducts(products);

                reachedEnd = products.size() < requestedPageSize
                        || products.isEmpty()
                        || (totalAvailable > 0 && adapter.getItemCount() >= totalAvailable);
                if (!reachedEnd) {
                    nextPage = requestedPage + 1;
                }

                updateLoadedStatus();
            }

            @Override
            public void onError(String message) {
                if (!isCurrentRequest(generation)) {
                    return;
                }

                loading = false;
                initialProgress.setVisibility(View.GONE);
                loadMoreProgress.setVisibility(View.GONE);
                statusText.setText(getString(R.string.load_error, message));
                retryButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean isCurrentRequest(int generation) {
        return generation == requestGeneration && !isFinishing() && !isDestroyed();
    }

    private void updateLoadedStatus() {
        if (adapter.getItemCount() == 0) {
            statusText.setText(R.string.no_products);
            return;
        }

        if (reachedEnd) {
            statusText.setText(getString(
                    R.string.all_products_loaded,
                    adapter.getItemCount(),
                    pageSize
            ));
            return;
        }

        String formattedTotal = totalAvailable > 0
                ? NumberFormat.getIntegerInstance().format(totalAvailable)
                : getString(R.string.unknown_total);
        statusText.setText(getString(
                R.string.products_loaded,
                adapter.getItemCount(),
                formattedTotal,
                pageSize
        ));
    }

    @Override
    protected void onDestroy() {
        api.shutdown();
        super.onDestroy();
    }
}
