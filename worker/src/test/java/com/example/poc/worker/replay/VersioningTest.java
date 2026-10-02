package com.example.poc.worker.replay;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventTypes;
import com.example.poc.handlers.customerregistered.CustomerRegistered;
import com.example.poc.handlers.customerregistered.CustomerRegisteredActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.worker.NonDeterministicException;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * {@code customer.registered-v1} was captured before the CRM sync was added. The guarded change in
 * CustomerRegisteredHandler replays it (see ReplayTest); the same change without getVersion
 * doesn't.
 */
class VersioningTest {

  /** CustomerRegisteredHandler's CRM sync change, minus the getVersion guard. */
  static class UnguardedCustomerRegisteredHandler implements EventHandler<CustomerRegistered> {
    private final CustomerRegisteredActivities activities =
        Workflow.newActivityStub(
            CustomerRegisteredActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public Class<CustomerRegistered> payloadType() {
      return CustomerRegistered.class;
    }

    @Override
    public String handle(CustomerRegistered customer) {
      activities.sendWelcomeEmail(customer);
      activities.syncToCrm(customer);
      return "Welcomed " + customer.customerId();
    }
  }

  @Test
  void guardedChangeReplaysPreChangeHistory() throws Exception {
    ReplayTestSupport.replay("customer.registered-v1");
  }

  @Test
  void unguardedChangeFailsToReplayPreChangeHistory() {
    assertThatThrownBy(
            () ->
                ReplayTestSupport.replayWithHandler(
                    "customer.registered-v1",
                    EventTypes.CUSTOMER_REGISTERED,
                    UnguardedCustomerRegisteredHandler::new))
        .hasMessageContaining(NonDeterministicException.class.getName());
  }

  @Test
  void guardedChangeReplaysPostChangeHistory() throws Exception {
    ReplayTestSupport.replay("customer.registered-v2");
  }
}
