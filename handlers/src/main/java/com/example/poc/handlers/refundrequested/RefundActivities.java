package com.example.poc.handlers.refundrequested;

import io.temporal.activity.ActivityInterface;

@ActivityInterface(namePrefix = "refund.requested.")
public interface RefundActivities {

  void requestApproval(RefundRequested refund);

  void issueRefund(RefundRequested refund);

  void notifyRejected(RefundRequested refund, String reason);
}
