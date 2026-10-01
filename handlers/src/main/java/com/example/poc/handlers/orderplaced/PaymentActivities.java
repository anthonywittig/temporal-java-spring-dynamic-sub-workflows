package com.example.poc.handlers.orderplaced;

import io.temporal.activity.ActivityInterface;

@ActivityInterface(namePrefix = "order.placed.")
public interface PaymentActivities {

  String PAYMENT_DECLINED = "PaymentDeclined";

  String chargePayment(OrderPlaced order);

  void refundPayment(String chargeId);
}
