package com.tanle.t_shorten_url.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class ShortUrlDetailResponse {
    private String id;
    private String shortCode;
    private String originalUrl;
    private Instant createdAt;
    private String qrCode;
}
