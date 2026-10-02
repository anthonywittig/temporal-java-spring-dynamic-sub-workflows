package com.example.poc.handlers.orderplaced;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventHandlerFactory;
import com.example.poc.api.EventTypes;

public class OrderPlacedHandlerFactory implements EventHandlerFactory {

  @Override
  public String eventType() {
    return EventTypes.ORDER_PLACED;
  }

  @Override
  public EventHandler<?> create() {
    return new OrderPlacedHandler();
  }
}
