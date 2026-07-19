package com.tanle.t_shorten_url.repository;

import com.tanle.t_shorten_url.entity.ShortAnalyzed;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortAnalyzedRepository extends MongoRepository<ShortAnalyzed, String> {
    List<ShortAnalyzed> findByShortUrlId(String shortUrlId);
}
