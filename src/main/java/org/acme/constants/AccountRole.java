package org.acme.constants;

import lombok.Getter;

@Getter
public enum AccountRole {
    USER("USER", "User role"),
    ADMIN("ADMIN", "Admin role");
    private final String value;
    private final String description;

    AccountRole(String value, String description) {
        this.value = value;
        this.description = description;
    }

}
