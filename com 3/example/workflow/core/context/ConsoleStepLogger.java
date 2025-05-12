package com.example.workflow.core;

import java.time.Instant;

public class ConsoleStepLogger implements StepLogger {
    @Override public void info(String step, String msg) {
        System.out.printf("%s [INFO] [%s] %s%n", Instant.now(), step, msg);
    }

    @Override public void error(String step, String msg, Throwable t) {
        System.err.printf("%s [ERROR] [%s] %s – %s%n", Instant.now(), step, msg, t);
    }
}