package com.example.poc.handlers.subscriptioncancelled;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventHandlerFactory;
import com.example.poc.api.EventTypes;

public class SubscriptionCancelledHandlerFactory implements EventHandlerFactory {

  @Override
  public String eventType() {
    return EventTypes.SUBSCRIPTION_CANCELLED;
  }

  @Override
  public EventHandler<?> create() {
    return new SubscriptionCancelledHandler();
  }
}
