package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class DataValidation {


    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&|])[A-Za-z\\d@$!%*?&|]{8,}$";

    public static final String PASSWORD_REQUIREMENTS_MESSAGE =
            "Password must meet the following criteria:\n" +
                    "- Minimum Length: At least 8 characters (or more depending on the standard).\n" +
                    "- Uppercase Letters: At least one uppercase letter (A-Z).\n" +
                    "- Lowercase Letters: At least one lowercase letter (a-z).\n" +
                    "- Digits: At least one digit (0-9).\n" +
                    "- Special Characters: At least one special character (e.g., !@#$%^&*()-_=+[]{}|;:'\",.<>?/).\n" +
                    "- No common patterns: Avoid sequential characters or common passwords.";

}
