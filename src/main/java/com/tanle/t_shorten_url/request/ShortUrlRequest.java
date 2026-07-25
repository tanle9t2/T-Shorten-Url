package com.tanle.t_shorten_url.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShortUrlRequest {
    private String shortCode;
    private String ipAddress;
    private String userAgent;
    private String referer;
}
