package com.example.workflow;

public record ComponentResult(Status status, Object data, String error) {
    public static ComponentResult ok(Object data) {
        return new ComponentResult(Status.SUCCESS, data, null);
    }
    public static ComponentResult error(String msg) {
        return new ComponentResult(Status.ERROR, null, msg);
    }
}
