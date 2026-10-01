package com.example.poc.handlers.orderplaced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.example.poc.handlers.WorkflowTestSupport;
import com.example.poc.testkit.Fixtures;
import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class OrderPlacedHandlerTest {

  private final InventoryActivities inventory =
      mock(InventoryActivities.class, withSettings().withoutAnnotations());
  private final PaymentActivities payment =
      mock(PaymentActivities.class, withSettings().withoutAnnotations());
  private final ShippingActivities shipping =
      mock(ShippingActivities.class, withSettings().withoutAnnotations());
  private final WorkflowTestSupport temporal =
      new WorkflowTestSupport(inventory, payment, shipping);

  @BeforeEach
  void setUp() {
    when(inventory.reserveInventory(any())).thenReturn("res-1");
    when(payment.chargePayment(any())).thenReturn("ch-1");
    when(shipping.createShipment(any())).thenReturn("shp-1");
  }

  @AfterEach
  void tearDown() {
    temporal.close();
  }

  @Test
  void runsAllStepsInOrder() {
    String result = temporal.run(Fixtures.event("order.placed"));

    assertThat(result).isEqualTo("Order o-2001 shipped as shp-1");
    InOrder order = inOrder(inventory, payment, shipping);
    order.verify(inventory).reserveInventory(any());
    order.verify(payment).chargePayment(any());
    order.verify(shipping).createShipment(any());
    verify(inventory, never()).releaseInventory(any());
    verify(payment, never()).refundPayment(any());
  }

  @Test
  void compensatesInReverseOrderWhenShippingFails() {
    when(shipping.createShipment(any()))
        .thenThrow(
            ApplicationFailure.newNonRetryableFailure(
                "no carrier", ShippingActivities.SHIPPING_UNAVAILABLE));

    assertThatThrownBy(() -> temporal.run(Fixtures.event("order.placed-shipping-fails")))
        .isInstanceOf(WorkflowFailedException.class)
        .cause()
        .isInstanceOf(ActivityFailure.class)
        .cause()
        .isInstanceOfSatisfying(
            ApplicationFailure.class,
            f -> assertThat(f.getType()).isEqualTo(ShippingActivities.SHIPPING_UNAVAILABLE));

    InOrder order = inOrder(payment, inventory);
    order.verify(payment).refundPayment("ch-1");
    order.verify(inventory).releaseInventory("res-1");
  }

  @Test
  void declinedPaymentIsNotRetriedAndOnlyReleasesInventory() {
    when(payment.chargePayment(any()))
        .thenThrow(
            ApplicationFailure.newFailure("declined", PaymentActivities.PAYMENT_DECLINED));

    assertThatThrownBy(() -> temporal.run(Fixtures.event("order.placed")))
        .isInstanceOf(WorkflowFailedException.class);

    // A plain (retryable) failure, so this checks the stub's doNotRetry list.
    verify(payment).chargePayment(any());
    verify(inventory).releaseInventory("res-1");
    verify(payment, never()).refundPayment(any());
    verify(shipping, never()).createShipment(any());
  }
}
