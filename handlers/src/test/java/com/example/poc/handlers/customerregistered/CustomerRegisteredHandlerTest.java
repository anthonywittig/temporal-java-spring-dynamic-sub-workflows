package com.example.poc.handlers.customerregistered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import com.example.poc.api.EventEnvelope;
import com.example.poc.handlers.WorkflowTestSupport;
import com.example.poc.testkit.Fixtures;
import com.example.poc.workflow.ProcessEventWorkflowImpl;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.common.SearchAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CustomerRegisteredHandlerTest {

  private final CustomerRegisteredActivities activities =
      mock(CustomerRegisteredActivities.class, withSettings().withoutAnnotations());
  private final WorkflowTestSupport temporal = new WorkflowTestSupport(activities);

  @AfterEach
  void tearDown() {
    temporal.close();
  }

  @Test
  void sendsWelcomeEmail() {
    EventEnvelope event = Fixtures.event("customer.registered");

    String result = temporal.run(event);

    assertThat(result).isEqualTo("Welcomed c-1001");
    verify(activities)
        .sendWelcomeEmail(new CustomerRegistered("c-1001", "ada@example.com", "Ada Lovelace"));
  }

  @Test
  void upsertsEventTypeSearchAttributeWhenStarterDidNotSetIt() {
    EventEnvelope event = Fixtures.event("customer.registered");
    temporal.run(event);

    SearchAttributes attributes =
        temporal
            .client
            .newUntypedWorkflowStub(event.workflowId())
            .describe()
            .getTypedSearchAttributes();
    assertThat(attributes.get(ProcessEventWorkflowImpl.EVENT_TYPE))
        .isEqualTo("customer.registered");
  }
}
