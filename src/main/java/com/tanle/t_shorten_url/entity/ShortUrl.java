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

@Document(collection = "short_url")
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class ShortUrl {
    @Id
    private String id;

    private String originalUrl;

    @Indexed(unique = true)
    private String shortCode;
    private Long views;
    private String qrCode;
    private boolean isActive;
    @Indexed
    private String userId;
    private Instant createdAt;
    private Instant updatedAt;
}
