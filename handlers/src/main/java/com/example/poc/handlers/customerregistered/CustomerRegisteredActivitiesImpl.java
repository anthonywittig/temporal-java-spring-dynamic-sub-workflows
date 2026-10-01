package com.example.poc.handlers.customerregistered;

import com.example.poc.api.PocConstants;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = PocConstants.TASK_QUEUE)
public class CustomerRegisteredActivitiesImpl implements CustomerRegisteredActivities {

  private static final Logger log = LoggerFactory.getLogger(CustomerRegisteredActivitiesImpl.class);

  @Override
  public void sendWelcomeEmail(CustomerRegistered customer) {
    log.info("Sending welcome email to {} <{}>", customer.name(), customer.email());
  }
}
