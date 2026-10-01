package com.example.poc.worker.replay;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventTypes;
import com.example.poc.handlers.orderplaced.InventoryActivities;
import com.example.poc.handlers.orderplaced.OrderPlaced;
import com.example.poc.handlers.orderplaced.PaymentActivities;
import com.example.poc.handlers.orderplaced.ShippingActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.worker.NonDeterministicException;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Proves the replay suite catches an unsafe handler change: {@code OrderPlacedHandler} with
 * payment charged before inventory is reserved.
 */
class NonDeterminismDetectionTest {

  /** OrderPlacedHandler with the first two activity calls swapped. Never do this without getVersion. */
  static class ReorderedOrderPlacedHandler implements EventHandler<OrderPlaced> {
    private final ActivityOptions options =
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build();
    private final InventoryActivities inventory =
        Workflow.newActivityStub(InventoryActivities.class, options);
    private final PaymentActivities payment =
        Workflow.newActivityStub(PaymentActivities.class, options);
    private final ShippingActivities shipping =
        Workflow.newActivityStub(ShippingActivities.class, options);

    @Override
    public Class<OrderPlaced> payloadType() {
      return OrderPlaced.class;
    }

    @Override
    public String handle(OrderPlaced order) {
      payment.chargePayment(order);
      inventory.reserveInventory(order);
      return "Order %s shipped as %s".formatted(order.orderId(), shipping.createShipment(order));
    }
  }

  @Test
  void reorderedActivitiesFailReplay() {
    assertThatThrownBy(
            () ->
                ReplayTestSupport.replayWithHandler(
                    "order.placed-success",
                    EventTypes.ORDER_PLACED,
                    ReorderedOrderPlacedHandler::new))
        // The replayer wraps the NonDeterministicException in a query failure.
        .hasMessageContaining(NonDeterministicException.class.getName())
        .hasMessageContaining("expected value 'name: \"order.placed.ChargePayment\"");
  }

  @Test
  void replayingTheOriginalHandlerThroughTheSameHarnessPasses() throws Exception {
    // Guards against the test above passing for the wrong reason (for example, a harness bug).
    ReplayTestSupport.replayWithHandler(
        "order.placed-success",
        EventTypes.ORDER_PLACED,
        com.example.poc.handlers.orderplaced.OrderPlacedHandler::new);
  }
}
