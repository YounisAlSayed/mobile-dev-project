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

import java.util.ArrayList;
import java.util.List;

/**
 * Main screen for a recycled e-commerce product feed backed by Open Library.
 */
public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int LOAD_MORE_THRESHOLD = 5;
    private static final String PAGE_SIZE_PREFERENCE = "page_size";

    private enum LoadDirection {
        FORWARD,
        BACKWARD
    }

    private final OpenLibraryProductApi api = new OpenLibraryProductApi();
    private ProductAdapter adapter;
    private TextView statusText;
    private Button retryButton;
    private ProgressBar initialProgress;
    private ProgressBar loadMoreProgress;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private int pageSize;
    private int requestGeneration;
    private long totalAvailable;
    private long windowStartIndex;
    private boolean loading;
    private boolean reachedEnd;
    private boolean userScrollActive;
    private LoadDirection lastLoadDirection = LoadDirection.FORWARD;

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
        layoutManager = new LinearLayoutManager(this);
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
                if (!userScrollActive || loading) {
                    return;
                }

                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (dy < 0
                        && windowStartIndex > 0
                        && firstVisible <= LOAD_MORE_THRESHOLD) {
                    loadPreviousPage();
                } else if (dy > 0
                        && !reachedEnd
                        && lastVisible >= adapter.getItemCount() - LOAD_MORE_THRESHOLD) {
                    loadNextPage();
                }
            }
        });

        findViewById(R.id.reloadButton).setOnClickListener(view -> resetAndLoad());

        retryButton.setOnClickListener(view -> {
            retryButton.setVisibility(View.GONE);
            if (adapter.getItemCount() == 0) {
                resetAndLoad();
            } else if (lastLoadDirection == LoadDirection.BACKWARD) {
                loadPreviousPage();
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
        totalAvailable = 0L;
        windowStartIndex = 0L;
        reachedEnd = false;
        loading = false;
        userScrollActive = false;
        retryButton.setVisibility(View.GONE);
        loadMoreProgress.setVisibility(View.GONE);
        statusText.setText(R.string.loading);
        loadNextPage();
    }

    private void loadNextPage() {
        if (loading || reachedEnd) {
            return;
        }

        loadPage(LoadDirection.FORWARD);
    }

    private void loadPreviousPage() {
        if (loading || windowStartIndex <= 0) {
            return;
        }

        loadPage(LoadDirection.BACKWARD);
    }

    private void loadPage(LoadDirection direction) {
        long requestWindowStart = windowStartIndex;
        long requestWindowEnd = requestWindowStart + adapter.getItemCount();
        long targetIndex = direction == LoadDirection.FORWARD
                ? requestWindowEnd
                : requestWindowStart - 1L;
        if (targetIndex < 0) {
            return;
        }

        int requestedPageSize = pageSize;
        int requestedPage = (int) (targetIndex / requestedPageSize) + 1;
        long pageStartIndex = (requestedPage - 1L) * requestedPageSize;

        loading = true;
        lastLoadDirection = direction;
        retryButton.setVisibility(View.GONE);
        statusText.setText(R.string.loading);

        if (adapter.getItemCount() == 0) {
            initialProgress.setVisibility(View.VISIBLE);
        } else {
            loadMoreProgress.setVisibility(View.VISIBLE);
        }

        final int generation = requestGeneration;

        api.fetchProducts(requestedPage, requestedPageSize, new OpenLibraryProductApi.Callback() {
            @Override
            public void onRetry(int nextAttempt, int maximumAttempts, long delayMillis) {
                if (!isCurrentRequest(generation)) {
                    return;
                }
            }

            @Override
            public void onSuccess(List<Product> products, long sourceTotal) {
                if (!isCurrentRequest(generation)) {
                    return;
                }

                initialProgress.setVisibility(View.GONE);
                loadMoreProgress.setVisibility(View.GONE);
                totalAvailable = sourceTotal;

                int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                View firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition);
                int firstVisibleOffset = firstVisibleView == null
                        ? 0
                        : firstVisibleView.getTop() - recyclerView.getPaddingTop();

                if (direction == LoadDirection.FORWARD) {
                    int fromIndex = (int) Math.max(
                            0L,
                            Math.min((long) products.size(), requestWindowEnd - pageStartIndex)
                    );
                    List<Product> missingProducts = new ArrayList<>(
                            products.subList(fromIndex, products.size())
                    );
                    int removedFromFront = adapter.appendProducts(missingProducts);
                    windowStartIndex = requestWindowStart + removedFromFront;

                    if (removedFromFront > 0
                            && firstVisiblePosition != RecyclerView.NO_POSITION) {
                        layoutManager.scrollToPositionWithOffset(
                                Math.max(0, firstVisiblePosition - removedFromFront),
                                firstVisibleOffset
                        );
                    }
                } else {
                    int toIndex = (int) Math.max(
                            0L,
                            Math.min((long) products.size(), requestWindowStart - pageStartIndex)
                    );
                    List<Product> missingProducts = new ArrayList<>(
                            products.subList(0, toIndex)
                    );
                    adapter.prependProducts(missingProducts);
                    windowStartIndex = Math.max(
                            0L,
                            requestWindowStart - missingProducts.size()
                    );

                    if (!missingProducts.isEmpty()
                            && firstVisiblePosition != RecyclerView.NO_POSITION) {
                        layoutManager.scrollToPositionWithOffset(
                                firstVisiblePosition + missingProducts.size(),
                                firstVisibleOffset
                        );
                    }
                }

                long retainedEnd = windowStartIndex + adapter.getItemCount();
                reachedEnd = totalAvailable > 0
                        ? retainedEnd >= totalAvailable
                        : direction == LoadDirection.FORWARD
                        && products.size() < requestedPageSize;

                loading = false;
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
                retryButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean isCurrentRequest(int generation) {
        return generation == requestGeneration && !isFinishing() && !isDestroyed();
    }

    private void updateLoadedStatus() {
        if (adapter.getItemCount() == 0) {
            return;
        }

        statusText.setText(getString(
                R.string.products_loaded_range,
                windowStartIndex,
                windowStartIndex + adapter.getItemCount()
        ));
    }

    @Override
    protected void onDestroy() {
        api.shutdown();
        super.onDestroy();
    }
}
