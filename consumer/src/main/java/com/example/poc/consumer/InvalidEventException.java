package com.example.poc.consumer;

/** The message can never be processed. It goes to the dead-letter topic. */
public class InvalidEventException extends RuntimeException {

  public InvalidEventException(String message) {
    super(message);
  }

  public InvalidEventException(String message, Throwable cause) {
    super(message, cause);
  }
}
