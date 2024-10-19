package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Query {

    public static final String FETCH_REFRESH_TOKEN = "select t from Token t where t.userId = ?1 order by t.id desc";
    public static final String EMAIL = "email";
    public static final String FETCH_PASSWORD_RESET_TOKEN = "select t from PasswordResets t where t.userId = ?1 order by t.id desc";
    public static final String UPDATE_PASSWORD = "passwordHash = ?1 where id = ?2";
}
