package com.example.poc.handlers.orderplaced;

import com.example.poc.api.PocConstants;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = PocConstants.TASK_QUEUE)
public class OrderPlacedActivitiesImpl
    implements InventoryActivities, PaymentActivities, ShippingActivities {

  private static final Logger log = LoggerFactory.getLogger(OrderPlacedActivitiesImpl.class);

  @Override
  public String reserveInventory(OrderPlaced order) {
    log.info("Reserving {} line items for order {}", order.items().size(), order.orderId());
    return "res-" + order.orderId();
  }

  @Override
  public void releaseInventory(String reservationId) {
    log.info("Releasing reservation {}", reservationId);
  }

  @Override
  public String chargePayment(OrderPlaced order) {
    if ("payment".equals(order.failAt())) {
      throw ApplicationFailure.newNonRetryableFailure(
          "Card declined for order " + order.orderId(), PAYMENT_DECLINED);
    }
    log.info("Charging {} cents for order {}", order.totalCents(), order.orderId());
    return "ch-" + order.orderId();
  }

  @Override
  public void refundPayment(String chargeId) {
    log.info("Refunding charge {}", chargeId);
  }

  @Override
  public String createShipment(OrderPlaced order) {
    if ("shipping".equals(order.failAt())) {
      throw ApplicationFailure.newNonRetryableFailure(
          "No carrier available for order " + order.orderId(), SHIPPING_UNAVAILABLE);
    }
    log.info("Creating shipment for order {}", order.orderId());
    return "shp-" + order.orderId();
  }
}
