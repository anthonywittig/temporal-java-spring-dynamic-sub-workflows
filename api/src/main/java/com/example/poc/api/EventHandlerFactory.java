package com.example.poc.api;

/**
 * Creates handlers for one event type. Discovered with {@link java.util.ServiceLoader}: list the
 * implementation in {@code META-INF/services/com.example.poc.api.EventHandlerFactory}.
 *
 * <p>{@link #create()} is called inside the workflow, so the handler may create activity stubs in
 * its constructor.
 */
public interface EventHandlerFactory {

  String eventType();

  EventHandler<?> create();
}
