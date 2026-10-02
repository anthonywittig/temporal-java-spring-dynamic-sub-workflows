package com.example.poc.handlers.customerregistered;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventHandlerFactory;
import com.example.poc.api.EventTypes;

public class CustomerRegisteredHandlerFactory implements EventHandlerFactory {

  @Override
  public String eventType() {
    return EventTypes.CUSTOMER_REGISTERED;
  }

  @Override
  public EventHandler<?> create() {
    return new CustomerRegisteredHandler();
  }
}
