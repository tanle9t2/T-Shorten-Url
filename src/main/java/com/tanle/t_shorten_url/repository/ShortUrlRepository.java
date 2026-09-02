package com.tanle.t_shorten_url.repository;

import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.projection.TotalViewProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ShortUrlRepository extends MongoRepository<ShortUrl, String> {
    @Query("{ 'shortCode' : ?0, 'isActive' : true }")
    Optional<ShortUrl> findByShortCode(String shortCode);

    @Query("{ '_id' : ?0, 'isActive' : true }")
    Optional<ShortUrl> findById(String id);

    @Query("{ 'shortCode' : ?0, 'isActive' : true }")
    Optional<TotalViewProjection> findTotalViews(String code);

    @Query("{ 'shortCode' : { $in: ?0 }, 'isActive' : true }")
    List<ShortUrl> findByShortCodeIn(Set<String> shortCodes);

    @Query("{ 'userId' : ?0, 'isActive' : true }")
    Page<ShortUrl> findByUserId(String userId, Pageable pageable);
}
