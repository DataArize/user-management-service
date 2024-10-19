package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ValidationErrors {

    public static final String EMAIL_IS_MANDATORY = "Email is a mandatory field";
    public static final String INVALID_EMAIL_FORMAT = "Invalid email format";
    public static final String FIRST_NAME_IS_MANDATORY = "First name is mandatory";
}
