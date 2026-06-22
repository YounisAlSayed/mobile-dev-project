package com.example.recyclerviewwebservice.network;

import android.os.Handler;
import android.os.Looper;

import com.example.recyclerviewwebservice.model.DemoPriceCalculator;
import com.example.recyclerviewwebservice.model.Product;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OpenLibraryProductApi {
    public static final String BASE_URL = "https://openlibrary.org/search.json";
    public static final String SEARCH_QUERY = "subject:fiction";
    private static final String COVER_BASE_URL = "https://covers.openlibrary.org/b";
    private static final String FIELDS =
            "key,title,author_name,cover_i,first_publish_year,edition_count";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object requestLock = new Object();
    private Future<?> activeRequest;
    private volatile HttpURLConnection activeConnection;

    public interface Callback {
        void onSuccess(List<Product> products, long totalItems);

        void onError(String message);

        default void onRetry(int nextAttempt, int maximumAttempts, long delayMillis) {
            // Optional UI progress callback.
        }
    }

    public void fetchProducts(int page, int pageSize, Callback callback) {
        synchronized (requestLock) {
            cancelActiveRequestLocked();
            activeRequest = executor.submit(() -> executeRequest(page, pageSize, callback));
        }
    }

    private void executeRequest(int page, int pageSize, Callback callback) {
        for (int attempt = 1; attempt <= NetworkRetryPolicy.MAX_ATTEMPTS; attempt++) {
            try {
                ProductPage result = performRequest(page, pageSize);
                mainHandler.post(() -> callback.onSuccess(
                        result.products,
                        result.totalItems
                ));
                return;
            } catch (Exception exception) {
                boolean canRetry = attempt < NetworkRetryPolicy.MAX_ATTEMPTS
                        && NetworkRetryPolicy.shouldRetry(exception)
                        && !Thread.currentThread().isInterrupted();

                if (!canRetry) {
                    postError(callback, exception);
                    return;
                }

                long delayMillis = NetworkRetryPolicy.delayAfterAttempt(attempt);
                int nextAttempt = attempt + 1;
                mainHandler.post(() -> callback.onRetry(
                        nextAttempt,
                        NetworkRetryPolicy.MAX_ATTEMPTS,
                        delayMillis
                ));

                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private ProductPage performRequest(int page, int pageSize) throws Exception {
        HttpURLConnection connection = null;
        try {
            String endpoint = String.format(
                    Locale.US,
                    "%s?q=%s&fields=%s&page=%d&limit=%d",
                    BASE_URL,
                    URLEncoder.encode(SEARCH_QUERY, StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(FIELDS, StandardCharsets.UTF_8.name()),
                    page,
                    pageSize
            );
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            activeConnection = connection;
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty(
                    "User-Agent",
                    "RecyclerViewProductFeed/3.0 (Android educational project)"
            );
            connection.setConnectTimeout(12_000);
            connection.setReadTimeout(20_000);

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("The Open Library server returned HTTP "
                        + responseCode + ".");
            }

            return parseResponse(readStream(connection.getInputStream()));
        } finally {
            if (activeConnection == connection) {
                activeConnection = null;
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ProductPage parseResponse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        long totalItems = root.optLong("numFound", root.optLong("num_found", 0L));
        JSONArray documents = root.getJSONArray("docs");
        List<Product> products = new ArrayList<>(documents.length());

        for (int index = 0; index < documents.length(); index++) {
            JSONObject document = documents.getJSONObject(index);
            String id = document.optString("key", "").trim();
            String title = document.optString("title", "Untitled product").trim();
            if (id.isEmpty()) {
                id = "/unknown/" + index + "/" + title.hashCode();
            }
            if (title.isEmpty()) {
                title = "Untitled product";
            }

            products.add(new Product(
                    id,
                    title,
                    parseAuthors(document.optJSONArray("author_name")),
                    buildCoverUrl(id, document.optLong("cover_i", 0L)),
                    DemoPriceCalculator.forProductId(id),
                    document.optInt("first_publish_year", 0),
                    document.optInt("edition_count", 0)
            ));
        }

        return new ProductPage(products, totalItems);
    }

    private String parseAuthors(JSONArray authorArray) {
        List<String> authors = new ArrayList<>();
        if (authorArray != null) {
            for (int index = 0; index < authorArray.length(); index++) {
                String author = authorArray.optString(index, "").trim();
                if (!author.isEmpty()) {
                    authors.add(author);
                }
            }
        }
        return authors.isEmpty() ? "Author not listed" : String.join(", ", authors);
    }

    private String buildCoverUrl(String productId, long coverId) {
        if (coverId > 0) {
            return COVER_BASE_URL + "/id/" + coverId + "-M.jpg";
        }
        String workId = productId.startsWith("/works/")
                ? productId.substring("/works/".length())
                : productId.replace("/", "");
        return COVER_BASE_URL + "/olid/" + workId + "-M.jpg?default=false";
    }

    private void postError(Callback callback, Exception exception) {
        String details = exception.getMessage();
        String message = (details == null || details.trim().isEmpty())
                ? "Could not retrieve products. Check the internet connection."
                : details;
        mainHandler.post(() -> callback.onError(message));
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    public void shutdown() {
        synchronized (requestLock) {
            cancelActiveRequestLocked();
        }
        executor.shutdownNow();
    }

    private void cancelActiveRequestLocked() {
        if (activeRequest != null && !activeRequest.isDone()) {
            activeRequest.cancel(true);
        }
        HttpURLConnection connection = activeConnection;
        activeConnection = null;
        if (connection != null) {
            connection.disconnect();
        }
    }

    private static final class ProductPage {
        final List<Product> products;
        final long totalItems;

        ProductPage(List<Product> products, long totalItems) {
            this.products = products;
            this.totalItems = totalItems;
        }
    }
}