package com.example.poc.handlers.refundrequested;

import com.example.poc.api.EventHandler;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Long-running: small refunds are issued immediately. Larger ones wait for a {@code refund.decide}
 * signal, and are rejected if no decision arrives within {@link #APPROVAL_TIMEOUT}.
 */
public class RefundRequestedHandler implements EventHandler<RefundRequested>, RefundApproval {

  public static final long AUTO_APPROVE_LIMIT_CENTS = 10_000;
  public static final Duration APPROVAL_TIMEOUT = Duration.ofDays(3);

  private final RefundActivities activities =
      Workflow.newActivityStub(
          RefundActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

  private ApprovalDecision decision;
  private String status = "RECEIVED";

  @Override
  public Class<RefundRequested> payloadType() {
    return RefundRequested.class;
  }

  @Override
  public String handle(RefundRequested refund) {
    if (refund.amountCents() > AUTO_APPROVE_LIMIT_CENTS) {
      status = "AWAITING_APPROVAL";
      activities.requestApproval(refund);
      boolean decided = Workflow.await(APPROVAL_TIMEOUT, () -> decision != null);
      if (!decided) {
        decision = new ApprovalDecision(false, "system", "approval timed out");
      }
    } else {
      decision = new ApprovalDecision(true, "system", "under auto-approve limit");
    }

    if (decision.approved()) {
      activities.issueRefund(refund);
      status = "REFUNDED";
      return "Refund %s issued (approved by %s)".formatted(refund.refundId(), decision.approver());
    }
    activities.notifyRejected(refund, decision.comment());
    status = "REJECTED";
    return "Refund %s rejected by %s: %s"
        .formatted(refund.refundId(), decision.approver(), decision.comment());
  }

  @Override
  public void decide(ApprovalDecision decision) {
    // First decision wins; later ones are ignored.
    if (this.decision == null) {
      this.decision = decision;
    }
  }

  @Override
  public String status() {
    return status;
  }
}
