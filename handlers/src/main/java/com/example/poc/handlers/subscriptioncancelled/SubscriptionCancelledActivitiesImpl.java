package com.example.poc.handlers.subscriptioncancelled;

import com.example.poc.api.PocConstants;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = PocConstants.TASK_QUEUE)
public class SubscriptionCancelledActivitiesImpl implements SubscriptionCancelledActivities {

  private static final Logger log =
      LoggerFactory.getLogger(SubscriptionCancelledActivitiesImpl.class);

  @Override
  public void revokeAccess(SubscriptionCancelled subscription) {
    log.info("Revoking {} access for {}", subscription.plan(), subscription.customerId());
  }

  @Override
  public void sendCancellationConfirmation(SubscriptionCancelled subscription) {
    log.info("Confirming cancellation of {}", subscription.subscriptionId());
  }
}
