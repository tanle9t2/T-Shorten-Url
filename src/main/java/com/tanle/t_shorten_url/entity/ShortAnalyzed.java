package com.tanle.t_shorten_url.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.ZonedDateTime;

@Document(collection = "short_analyzed")
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class ShortAnalyzed {

    @Id
    private String id;

    @Indexed
    private String shortCode;

    private String userId;

    private String ipAddress;
    private String userAgent;
    private String referer;
    private String country;
    private String os;
    private String browser;

    @Indexed(unique = true)
    private String eventId;
    private Instant createdAt;
}
