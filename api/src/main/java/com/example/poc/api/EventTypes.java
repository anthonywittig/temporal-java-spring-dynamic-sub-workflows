package com.example.poc.api;

import java.util.Set;

/**
 * The expected set of event types, shared by the consumer and the worker so each can check at
 * startup that it agrees with the other (see the registry consistency test and the consumer's
 * startup check). The consumer only routes types in this set; the worker refuses to start if its
 * registered handlers don't match it exactly.
 */
public final class EventTypes {

  public static final String CUSTOMER_REGISTERED = "customer.registered";
  public static final String ORDER_PLACED = "order.placed";
  public static final String REFUND_REQUESTED = "refund.requested";
  public static final String SUBSCRIPTION_CANCELLED = "subscription.cancelled";

  public static final Set<String> ALL =
      Set.of(CUSTOMER_REGISTERED, ORDER_PLACED, REFUND_REQUESTED, SUBSCRIPTION_CANCELLED);

  private EventTypes() {}
}
