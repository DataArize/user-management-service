package org.acme.constants;

import lombok.Getter;

@Getter
public enum AccountStatus {
    ACTIVE("ACTIVE", "Account is in active state"),
    SUSPENDED("SUSPENDED", "Account is in suspended state");

    private final String value;
    private final String description;

    AccountStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
