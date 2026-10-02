package com.example.poc.handlers.orderplaced;

import io.temporal.activity.ActivityInterface;

@ActivityInterface(namePrefix = "order.placed.")
public interface ShippingActivities {

  String SHIPPING_UNAVAILABLE = "ShippingUnavailable";

  String createShipment(OrderPlaced order);
}
