package com.example.poc.handlers.refundrequested;

import com.example.poc.api.PocConstants;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = PocConstants.TASK_QUEUE)
public class RefundActivitiesImpl implements RefundActivities {

  private static final Logger log = LoggerFactory.getLogger(RefundActivitiesImpl.class);

  @Override
  public void requestApproval(RefundRequested refund) {
    log.info(
        "Asking a manager to approve refund {} of {} cents", refund.refundId(), refund.amountCents());
  }

  @Override
  public void issueRefund(RefundRequested refund) {
    log.info("Issuing refund {} of {} cents", refund.refundId(), refund.amountCents());
  }

  @Override
  public void notifyRejected(RefundRequested refund, String reason) {
    log.info("Refund {} rejected: {}", refund.refundId(), reason);
  }
}
