package com.tanle.t_shorten_url.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShortUrlCreatedRequest {
    private String originalUrl;
    @JsonProperty("isWithQRCode")
    private boolean withQRCode;
}
