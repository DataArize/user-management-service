package org.acme.constants;

public enum AttemptType {
    SUCCESS("SUCCESS", "Login attempt successfull"),
    FAILED("FAILED", "Login attempt failed");

    private final String value;
    private final String description;

    AttemptType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
