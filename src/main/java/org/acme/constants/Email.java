package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Email {

    public static final String SUBJECT = "Reset Your Password";
    public static final String BODY = """
        Hello,
        
        We received a request to reset your password. Please click the link below to reset your password:
        
        #RESET_LINK
        
        If you didn't request this, please ignore this email.
        
        Thanks,
        The House of LLM Team
        """;
    public static final String RESET_LINK = "#RESET_LINK";
    public static final String BASE_URL = "http://localhost:8080/reset-password?token=";
}
