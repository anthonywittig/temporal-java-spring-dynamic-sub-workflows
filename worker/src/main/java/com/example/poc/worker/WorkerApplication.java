package com.example.poc.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Scans com.example.poc so activity beans in the handlers module are picked up without listing them.
@SpringBootApplication(scanBasePackages = "com.example.poc")
public class WorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(WorkerApplication.class, args);
  }
}
