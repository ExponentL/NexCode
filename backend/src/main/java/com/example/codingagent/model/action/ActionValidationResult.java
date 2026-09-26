package com.example.codingagent.model.action;

public record ActionValidationResult(
        boolean valid,
        String errorMessage,
        String resolvedTarget
) {
    public static ActionValidationResult ok() {
        return new ActionValidationResult(true, null, null);
    }

    public static ActionValidationResult ok(String resolvedTarget) {
        return new ActionValidationResult(true, null, resolvedTarget);
    }

    public static ActionValidationResult invalid(String errorMessage) {
        return new ActionValidationResult(false, errorMessage, null);
    }
}
