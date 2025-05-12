package com.example.workflow.core;

public interface StepLogger {
    void info(String step, String msg);
    void error(String step, String msg, Throwable t);
}