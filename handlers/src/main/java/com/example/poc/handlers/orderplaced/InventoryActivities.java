package com.example.poc.handlers.orderplaced;

import io.temporal.activity.ActivityInterface;

@ActivityInterface(namePrefix = "order.placed.")
public interface InventoryActivities {

  String reserveInventory(OrderPlaced order);

  void releaseInventory(String reservationId);
}
