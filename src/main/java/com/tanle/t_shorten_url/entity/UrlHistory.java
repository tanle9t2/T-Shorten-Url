package com.tanle.t_shorten_url.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.ZonedDateTime;

@Document(collection = "url_history")
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class UrlHistory {
    @Id
    private String id;

    private String oldUrl;
    private String newUrl;

    private String urlShortId;
    private String userId;
    private Instant createdAt;
}
