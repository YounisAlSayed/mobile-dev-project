# RecyclerShop Product Feed (Java)

An Android Studio Java e-commerce prototype whose main activity uses the official AndroidX `RecyclerView` to display a large, complex, image-backed product feed.

## Requirement coverage

- Large source: Open Library's fiction query currently reports more than 1.6 million works.
- Product object: ID, title, author/subtitle, cover URL, prototype price, publication metadata, and favorite state.
- Complex card: URL image, title, price, metadata, and interactive favorite heart.
- Image loading: Glide 5 handles asynchronous URL loading, placeholders, memory/disk caching, and recycled request cleanup.
- Smooth favorite updates: a RecyclerView payload updates only the heart instead of rebinding the image and full card.
- Persisted favorites: favorite product IDs survive pack reloads and app restarts through `SharedPreferences`.
- View recycling: `ProductAdapter` and `ProductViewHolder` use the standard RecyclerView contract; `onViewRecycled` clears obsolete Glide targets.
- User-selectable packs: 10, 25, 50, or 100 products.
- Rolling adapter window: at most 150 `Product` objects are retained; only the exact overflow is removed from the opposite end.
- Bidirectional paging: scrolling down fetches newer discarded rows, while scrolling back up fetches and prepends older discarded rows from the API.
- Visible memory status: the header reports the retained source range and current adapter-object count.
- Full reload: changing pack size or pressing the header reload button clears rows, cancels obsolete work, returns to page one, and requests a fresh pack.

## Data and pricing

Products are derived from Open Library works using `https://openlibrary.org/search.json`. Cover images use the official Open Library Covers API. Open Library is a catalog rather than a store, so prices are deterministic **prototype prices** generated locally from each stable work ID; they are not real retail prices.

## Run

1. Open this folder in Android Studio.
2. Sync Gradle and select an internet-connected emulator/device.
3. Run the `app` configuration.
4. Scroll to recycle cards and load another pack, change **Items per pack**, tap hearts, or use the header reload icon.

The rolling window can move in both directions while keeping no more than 150 product objects in the adapter.
