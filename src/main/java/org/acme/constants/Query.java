package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Query {

    public static final String FETCH_REFRESH_TOKEN = "select t from Token t where t.userId = ?1 order by t.id desc";
}
