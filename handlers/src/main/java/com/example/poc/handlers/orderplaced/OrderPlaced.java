package com.example.poc.handlers.orderplaced;

import java.util.List;

/**
 * @param failAt optional demo hook: {@code payment} or {@code shipping} makes that step fail
 *     permanently so the compensation path runs
 */
public record OrderPlaced(
    String orderId, String customerId, List<LineItem> items, long totalCents, String failAt) {

  public record LineItem(String sku, int quantity) {}
}
