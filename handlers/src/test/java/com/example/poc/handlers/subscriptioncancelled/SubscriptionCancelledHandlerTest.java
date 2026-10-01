package com.example.poc.handlers.subscriptioncancelled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.example.poc.handlers.WorkflowTestSupport;
import com.example.poc.testkit.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class SubscriptionCancelledHandlerTest {

  private final SubscriptionCancelledActivities activities =
      mock(SubscriptionCancelledActivities.class, withSettings().withoutAnnotations());
  private final WorkflowTestSupport temporal = new WorkflowTestSupport(activities);

  @AfterEach
  void tearDown() {
    temporal.close();
  }

  @Test
  void revokesAccessThenConfirms() {
    assertThat(temporal.run(Fixtures.event("subscription.cancelled"))).isEqualTo("Cancelled s-4001");

    SubscriptionCancelled subscription = new SubscriptionCancelled("s-4001", "c-1001", "pro");
    InOrder order = inOrder(activities);
    order.verify(activities).revokeAccess(subscription);
    order.verify(activities).sendCancellationConfirmation(subscription);
  }
}
