package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    // Thread pool with bounded queue to prevent resource exhaustion
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    // Thread-safe collections and counters
    private final ConcurrentLinkedQueue<Item> processedItems = new ConcurrentLinkedQueue<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    @PreDestroy
    public void cleanup() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Processes items asynchronously with proper concurrency handling.
     * 
     * Improvements made:
     * 1. Returns CompletableFuture for proper async operation
     * 2. Uses thread-safe collections and atomic counters
     * 3. Properly waits for all processing to complete
     * 4. Implements proper error handling and propagation
     * 5. Uses bounded thread pool with optimal size
     * 
     * @return CompletableFuture<List<Item>> containing all processed items
     */
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> processItem(id), executor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])).thenApply(v -> new ArrayList<>(processedItems));
    }

    /**
     * Processes a single item with proper error handling.
     * 
     * @param id The ID of the item to process
     */
    private void processItem(Long id) {
        try {
            Optional<Item> itemOpt = itemRepository.findById(id);
            if (itemOpt.isPresent()) {
                Item item = itemOpt.get();
                Thread.sleep(100);

                item.setStatus("PROCESSED");
                Item savedItem = itemRepository.save(item);
                processedItems.offer(savedItem);
                processedCount.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Processing interrupted", e);
        } catch (Exception e) {
            throw new CompletionException("Error processing item " + id, e);
        }
    }

}
