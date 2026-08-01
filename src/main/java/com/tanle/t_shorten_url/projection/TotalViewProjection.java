package com.tanle.t_shorten_url.projection;

public interface TotalViewProjection {
    String getId();

    String getShortCode();

    String getOriginalUrl();

    Long getViews();

}
