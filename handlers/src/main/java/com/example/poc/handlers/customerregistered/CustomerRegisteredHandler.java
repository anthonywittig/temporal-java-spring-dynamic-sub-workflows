package com.example.poc.handlers.customerregistered;

import com.example.poc.api.EventHandler;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/** Simple: one activity, no branching. */
public class CustomerRegisteredHandler implements EventHandler<CustomerRegistered> {

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
    return "Welcomed " + customer.customerId();
  }
}
