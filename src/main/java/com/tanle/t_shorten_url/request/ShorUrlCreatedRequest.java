package com.tanle.t_shorten_url.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
public class ShorUrlCreatedRequest {
    private String originalUrl;
}
