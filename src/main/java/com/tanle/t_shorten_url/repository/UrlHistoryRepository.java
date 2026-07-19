package com.tanle.t_shorten_url.repository;

import com.tanle.t_shorten_url.entity.UrlHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlHistoryRepository extends MongoRepository<UrlHistory, String> {
    List<UrlHistory> findUrlHistoriesByUrlShortId(String urlShortId);
}
