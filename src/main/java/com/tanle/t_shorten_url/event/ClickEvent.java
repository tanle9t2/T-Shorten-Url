package com.tanle.t_shorten_url.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
public record ClickEvent(
        String eventId,
        String shortCode,
        Instant createdAt,
        String ipAddress,
        String userAgent,
        String referer) {
}
