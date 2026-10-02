package com.example.poc.handlers.refundrequested;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.PocConstants;
import com.example.poc.handlers.WorkflowTestSupport;
import com.example.poc.testkit.Fixtures;
import io.temporal.client.WorkflowStub;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RefundRequestedHandlerTest {

  private final RefundActivities activities =
      mock(RefundActivities.class, withSettings().withoutAnnotations());
  private final WorkflowTestSupport temporal = new WorkflowTestSupport(activities);

  @AfterEach
  void tearDown() {
    temporal.close();
  }

  @Test
  void smallRefundIsAutoApproved() {
    String result = temporal.run(Fixtures.event("refund.requested-small"));

    assertThat(result).isEqualTo("Refund r-3001 issued (approved by system)");
    verify(activities, never()).requestApproval(any());
    verify(activities).issueRefund(any());
  }

  @Test
  void largeRefundWaitsForApprovalSignal() {
    WorkflowStub stub = temporal.start(Fixtures.event("refund.requested-large"));

    await()
        .atMost(Duration.ofSeconds(10))
        .until(() -> "AWAITING_APPROVAL".equals(stub.query(RefundApproval.STATUS, String.class)));
    verify(activities).requestApproval(any());
    verify(activities, never()).issueRefund(any());

    stub.signal(RefundApproval.DECIDE, new ApprovalDecision(true, "mgr-ana", "ok"));

    assertThat(stub.getResult(String.class)).isEqualTo("Refund r-3002 issued (approved by mgr-ana)");
    assertThat(stub.query(RefundApproval.STATUS, String.class)).isEqualTo("REFUNDED");
    verify(activities).issueRefund(any());
  }

  @Test
  void rejectionNotifiesCustomer() {
    WorkflowStub stub = temporal.start(Fixtures.event("refund.requested-large"));
    stub.signal(RefundApproval.DECIDE, new ApprovalDecision(false, "mgr-ana", "outside policy"));

    assertThat(stub.getResult(String.class))
        .isEqualTo("Refund r-3002 rejected by mgr-ana: outside policy");
    verify(activities).notifyRejected(any(), eq("outside policy"));
    verify(activities, never()).issueRefund(any());
  }

  @Test
  void signalSentWithStartIsBufferedUntilHandlerRegisters() {
    EventEnvelope event = Fixtures.event("refund.requested-large");
    WorkflowStub stub =
        temporal.client.newUntypedWorkflowStub(
            PocConstants.WORKFLOW_TYPE, WorkflowTestSupport.options(event));

    stub.signalWithStart(
        RefundApproval.DECIDE,
        new Object[] {new ApprovalDecision(true, "mgr-ana", "pre-approved")},
        new Object[] {event});

    assertThat(stub.getResult(String.class)).isEqualTo("Refund r-3002 issued (approved by mgr-ana)");
  }

  @Test
  void rejectsWhenNoDecisionBeforeTimeout() {
    WorkflowStub stub = temporal.start(Fixtures.event("refund.requested-large"));

    // The test server skips the 3-day timer while we block on the result.
    assertThat(stub.getResult(String.class))
        .isEqualTo("Refund r-3002 rejected by system: approval timed out");
    verify(activities).notifyRejected(any(), eq("approval timed out"));
  }
}
