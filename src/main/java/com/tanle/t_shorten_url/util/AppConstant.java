package com.tanle.t_shorten_url.util;

public class AppConstant {
    // TD-001: findRedirectUrl receives no user until login exists.
    public static final String ANONYMOUS_USER_ID = "anonymous";
    // Historical spelling is kept: stored clicks already contain it.
    public static final String NO_REFERER = "Unknow";
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";
}
