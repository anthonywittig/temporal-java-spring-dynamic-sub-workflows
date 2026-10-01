package com.example.poc.handlers.customerregistered;

import com.example.poc.api.EventHandler;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Simple: one activity, no branching.
 *
 * <p>Version 1 added a CRM sync after the welcome email. Executions that recorded the welcome
 * email before that change replay at {@link Workflow#DEFAULT_VERSION} and skip it.
 */
public class CustomerRegisteredHandler implements EventHandler<CustomerRegistered> {

  // Prefixed with the event type: all handlers share one workflow type, so keep change IDs unique.
  static final String CRM_SYNC_CHANGE = "customer.registered.crm-sync";

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
    if (Workflow.getVersion(CRM_SYNC_CHANGE, Workflow.DEFAULT_VERSION, 1) >= 1) {
      activities.syncToCrm(customer);
    }
    return "Welcomed " + customer.customerId();
  }
}
