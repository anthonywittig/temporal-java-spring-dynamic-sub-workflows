package com.example.poc.handlers.refundrequested;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventHandlerFactory;
import com.example.poc.api.EventTypes;

public class RefundRequestedHandlerFactory implements EventHandlerFactory {

  @Override
  public String eventType() {
    return EventTypes.REFUND_REQUESTED;
  }

  @Override
  public EventHandler<?> create() {
    return new RefundRequestedHandler();
  }
}
