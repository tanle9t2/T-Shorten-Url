package com.tanle.t_shorten_url.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TotalViewUrlResponse {
    private String id;
    private String shortCode;
    private String originalUrl;
    private Long views;
}
