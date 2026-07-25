package com.tanle.t_shorten_url.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShortUrlUpdateRequest {
    private String id;
    private String originalUrl;
}
