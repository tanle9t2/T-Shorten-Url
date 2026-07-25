package com.tanle.t_shorten_url.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.ZonedDateTime;

@Document(collection = "user")
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;
    private String username;
    private String password;

    private String firstName;
    private String lastName;

    private Instant createdAt;
    private Instant updatedAt;
}
