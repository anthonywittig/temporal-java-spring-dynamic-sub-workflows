package com.example.poc.handlers.refundrequested;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;

/**
 * Signals and queries owned by the refund handler. Not a workflow interface: the workflow
 * registers the handler as a listener at runtime. Clients call these by name through an untyped
 * stub (or {@code temporal workflow signal --name refund.decide}).
 *
 * <p>Names are prefixed with the event type so they can't collide with another handler's.
 */
public interface RefundApproval {

  String DECIDE = "refund.decide";
  String STATUS = "refund.status";

  @SignalMethod(name = DECIDE)
  void decide(ApprovalDecision decision);

  @QueryMethod(name = STATUS)
  String status();
}
