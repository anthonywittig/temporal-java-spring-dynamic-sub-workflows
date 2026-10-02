package com.example.poc.workflow;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventHandlerFactory;
import io.temporal.failure.ApplicationFailure;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/** Immutable map of event type to handler factory. */
public final class HandlerRegistry {

  public static final String UNKNOWN_EVENT_TYPE = "UnknownEventType";

  private static final HandlerRegistry DEFAULT = fromServiceLoader();

  private final Map<String, EventHandlerFactory> factories;

  private HandlerRegistry(Map<String, EventHandlerFactory> factories) {
    this.factories = Map.copyOf(factories);
  }

  /** The registry built from {@code META-INF/services} entries on the classpath. */
  public static HandlerRegistry defaultRegistry() {
    return DEFAULT;
  }

  public static HandlerRegistry of(List<? extends EventHandlerFactory> factories) {
    Map<String, EventHandlerFactory> byType = new HashMap<>();
    for (EventHandlerFactory factory : factories) {
      EventHandlerFactory previous = byType.put(factory.eventType(), factory);
      if (previous != null) {
        throw new IllegalStateException(
            "Duplicate handler factories for event type '%s': %s and %s"
                .formatted(
                    factory.eventType(),
                    previous.getClass().getName(),
                    factory.getClass().getName()));
      }
    }
    return new HandlerRegistry(byType);
  }

  private static HandlerRegistry fromServiceLoader() {
    return of(
        ServiceLoader.load(EventHandlerFactory.class, HandlerRegistry.class.getClassLoader())
            .stream()
            .map(ServiceLoader.Provider::get)
            .toList());
  }

  public Set<String> eventTypes() {
    return factories.keySet();
  }

  /**
   * Creates a new handler for {@code type}. Must be called from workflow code.
   *
   * @throws ApplicationFailure (non-retryable) if no factory is registered for the type
   */
  public EventHandler<?> forType(String type) {
    EventHandlerFactory factory = factories.get(type);
    if (factory == null) {
      throw ApplicationFailure.newNonRetryableFailure(
          "No handler registered for event type '" + type + "'", UNKNOWN_EVENT_TYPE);
    }
    return factory.create();
  }
}
