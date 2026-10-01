package com.example.poc.handlers.subscriptioncancelled;

import com.example.poc.api.EventHandler;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class SubscriptionCancelledHandler implements EventHandler<SubscriptionCancelled> {

  private final SubscriptionCancelledActivities activities =
      Workflow.newActivityStub(
          SubscriptionCancelledActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public Class<SubscriptionCancelled> payloadType() {
    return SubscriptionCancelled.class;
  }

  @Override
  public String handle(SubscriptionCancelled subscription) {
    activities.revokeAccess(subscription);
    activities.sendCancellationConfirmation(subscription);
    return "Cancelled " + subscription.subscriptionId();
  }
}
