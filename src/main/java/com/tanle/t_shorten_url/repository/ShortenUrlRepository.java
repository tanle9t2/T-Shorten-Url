package com.tanle.t_shorten_url.repository;

import com.tanle.t_shorten_url.entity.ShortUrl;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShortenUrlRepository extends MongoRepository<ShortUrl, String> {
}
