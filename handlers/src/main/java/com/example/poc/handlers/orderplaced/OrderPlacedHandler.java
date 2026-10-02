package com.example.poc.handlers.orderplaced;

import com.example.poc.api.EventHandler;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Multi-step: reserve inventory, charge payment, create shipment. Each step has its own timeout
 * and retry policy. If a step fails, completed steps are compensated in reverse order and the
 * workflow fails.
 */
public class OrderPlacedHandler implements EventHandler<OrderPlaced> {

  // Fast, local system: short timeout, a few quick retries.
  private final InventoryActivities inventory =
      Workflow.newActivityStub(
          InventoryActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(5))
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
              .build());

  // External payment provider: longer timeout, backoff, and declines are not retried.
  private final PaymentActivities payment =
      Workflow.newActivityStub(
          PaymentActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setInitialInterval(Duration.ofSeconds(2))
                      .setBackoffCoefficient(2.0)
                      .setMaximumAttempts(5)
                      .setDoNotRetry(PaymentActivities.PAYMENT_DECLINED)
                      .build())
              .build());

  // Carrier API can be slow; bound the total time across retries.
  private final ShippingActivities shipping =
      Workflow.newActivityStub(
          ShippingActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(60))
              .setScheduleToCloseTimeout(Duration.ofMinutes(10))
              .build());

  @Override
  public Class<OrderPlaced> payloadType() {
    return OrderPlaced.class;
  }

  @Override
  public String handle(OrderPlaced order) {
    Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
    try {
      String reservationId = inventory.reserveInventory(order);
      saga.addCompensation(inventory::releaseInventory, reservationId);

      String chargeId = payment.chargePayment(order);
      saga.addCompensation(payment::refundPayment, chargeId);

      String shipmentId = shipping.createShipment(order);
      return "Order %s shipped as %s".formatted(order.orderId(), shipmentId);
    } catch (RuntimeException e) {
      saga.compensate();
      throw e;
    }
  }
}
