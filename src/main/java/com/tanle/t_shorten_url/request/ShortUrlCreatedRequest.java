package com.tanle.t_shorten_url.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShortUrlCreatedRequest {
    private String originalUrl;
}
