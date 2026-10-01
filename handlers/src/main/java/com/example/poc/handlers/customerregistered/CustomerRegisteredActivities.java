package com.example.poc.handlers.customerregistered;

import io.temporal.activity.ActivityInterface;

// The prefix keeps activity type names unique across all handlers on the shared task queue.
@ActivityInterface(namePrefix = "customer.registered.")
public interface CustomerRegisteredActivities {

  void sendWelcomeEmail(CustomerRegistered customer);

  void syncToCrm(CustomerRegistered customer);
}
