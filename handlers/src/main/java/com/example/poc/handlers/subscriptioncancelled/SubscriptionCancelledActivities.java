package com.example.poc.handlers.subscriptioncancelled;

import io.temporal.activity.ActivityInterface;

@ActivityInterface(namePrefix = "subscription.cancelled.")
public interface SubscriptionCancelledActivities {

  void revokeAccess(SubscriptionCancelled subscription);

  void sendCancellationConfirmation(SubscriptionCancelled subscription);
}
