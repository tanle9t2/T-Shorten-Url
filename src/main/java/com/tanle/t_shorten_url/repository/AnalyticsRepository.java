package com.tanle.t_shorten_url.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {
    private final MongoTemplate mongoTemplate;

    public record ClickCount(String shortCode, String day, long clicks) {
    }

    public record ValueCount(String value, long clicks) {
    }

    // field must be a trusted constant, never request input.
    public List<ValueCount> countClicksBy(String field, String shortCode, Instant fromInclusive, Instant toExclusive) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("shortCode").is(shortCode)
                        .and("createdAt").gte(fromInclusive).lt(toExclusive)),
                Aggregation.group(field).count().as("clicks"),
                Aggregation.project("clicks").and("value").previousOperation().andExclude("_id"))
                // Raw referers and user agents can be high-cardinality; avoid Mongo's 100MB group limit.
                .withOptions(AggregationOptions.builder().allowDiskUse(true).build());

        return mongoTemplate.aggregate(aggregation, "short_analyzed", ValueCount.class).getMappedResults();
    }

    // Grouping by (shortCode, day) keeps one query serving the series, the summary and the top links.
    public List<ClickCount> countClicksByDay(Collection<String> shortCodes, Instant fromInclusive,
                                             Instant toExclusive, ZoneId zone) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("shortCode").in(shortCodes)
                        .and("createdAt").gte(fromInclusive).lt(toExclusive)),
                Aggregation.project("shortCode")
                        .and(DateOperators.dateOf("createdAt")
                                .withTimezone(DateOperators.Timezone.valueOf(zone.getId()))
                                .toString("%Y-%m-%d"))
                        .as("day"),
                Aggregation.group("shortCode", "day").count().as("clicks"),
                Aggregation.project("shortCode", "day", "clicks").andExclude("_id"));

        return mongoTemplate.aggregate(aggregation, "short_analyzed", ClickCount.class).getMappedResults();
    }
}
