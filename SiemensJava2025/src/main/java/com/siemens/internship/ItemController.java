package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        List<Item> items = itemService.findAll();
        return items.isEmpty()
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(items, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Item savedItem = itemService.save(item);
        return new ResponseEntity<>(savedItem, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody Item item) {
        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            item.setId(id);
            return new ResponseEntity<>(itemService.save(item), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (!itemService.findById(id).isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        itemService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/batch-process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService.processItemsAsync()
                .thenApply(processedItems -> processedItems.isEmpty()
                        ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                        : new ResponseEntity<>(processedItems, HttpStatus.OK));
    }
}
