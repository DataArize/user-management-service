package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class JwtToken {

    public static final String CLAIM_TYPE = "type";
    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";
    public static final String AUDIENCE = "EMAIL_SERVER";
    public static final String ISSUER = "https://houseofllm.com";
}