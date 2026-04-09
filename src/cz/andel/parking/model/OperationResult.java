package cz.andel.parking.model;

import java.util.List;

public class OperationResult {
    private final boolean success;
    private final String message;
    private final List<String> details;

    private OperationResult(boolean success, String message, List<String> details) {
        this.success = success;
        this.message = message;
        this.details = List.copyOf(details);
    }

    public static OperationResult success(String message, List<String> details) {
        return new OperationResult(true, message, details);
    }

    public static OperationResult failure(String message, List<String> details) {
        return new OperationResult(false, message, details);
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public List<String> details() {
        return details;
    }
}
