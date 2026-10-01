package com.example.poc.worker;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.PocConstants;
import com.example.poc.testkit.Fixtures;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the real worker against the in-memory test server to check that auto-discovery finds the
 * workflow and every handler's activity beans with no per-type worker config.
 */
@SpringBootTest(properties = "spring.temporal.test-server.enabled=true")
class WorkerWiringTest {

  @Autowired TestWorkflowEnvironment env;
  @Autowired WorkflowClient client;

  @Test
  void runsEveryEventTypeThroughSpringWiredActivities() {
    env.registerSearchAttribute(
        PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);

    assertThat(run(Fixtures.event("customer.registered"))).isEqualTo("Welcomed c-1001");
    assertThat(run(Fixtures.event("order.placed"))).isEqualTo("Order o-2001 shipped as shp-o-2001");
    assertThat(run(Fixtures.event("refund.requested-small")))
        .isEqualTo("Refund r-3001 issued (approved by system)");
  }

  private String run(EventEnvelope event) {
    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            PocConstants.WORKFLOW_TYPE,
            WorkflowOptions.newBuilder()
                .setWorkflowId(event.workflowId())
                .setTaskQueue(PocConstants.TASK_QUEUE)
                .build());
    stub.start(event);
    return stub.getResult(String.class);
  }
}
