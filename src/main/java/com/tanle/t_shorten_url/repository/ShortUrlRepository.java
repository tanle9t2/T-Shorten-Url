package com.tanle.t_shorten_url.repository;

import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.projection.TotalViewProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ShortUrlRepository extends MongoRepository<ShortUrl, String> {
    Optional<ShortUrl> findByShortCode(String shortCode);

    @Query("{ 'shortCode' : ?0 }")
    Optional<TotalViewProjection> findTotalViews(String code);

    List<ShortUrl> findByShortCodeIn(Set<String> shortCodes);
}
