# System Architecture

## System Overview

This system is a distributed URL Shortener platform designed for high read throughput, low latency redirection, and asynchronous analytics processing.

The main responsibilities are:

* Generate shortened URLs.
* Redirect users with minimal latency.
* Collect click analytics asynchronously.
* Reduce database load using caching.
* Handle high traffic through horizontal scaling.

---

# High-Level Architecture

```
                    Client
                       |
                       |
                       v

                Load Balancer

                       |
                       |
                       v

                 URL Service

              +--------+--------+
              |                 |
              v                 v

            Redis          Kafka Producer

              |                 |
              |                 |
              v                 v

            MongoDB        Kafka Cluster

                                |
                                |
                                v

                    Analytics Consumer

                                |
                                |
                                v

                         Redis Counter

                                |
                       Flush periodically

                                |
                                |
                                v

                              MongoDB
```

---

# Component Responsibilities

---

# Client

## Responsibility

The client sends:

* URL creation request.
* Redirect request.
* Analytics request.

The client does not directly communicate with databases or Kafka.

---

# Load Balancer

## Responsibility

Distribute traffic across multiple URL Service instances.

Responsibilities:

* Traffic distribution.
* Health checking.
* SSL termination.
* Failover.

---

# URL Service

## Responsibility

Core business service.

Handles:

* URL shortening.
* Redirect.
* URL validation.
* URL expiration.
* Analytics event publishing.

---

## URL Creation Flow

Flow:

```
Client

↓

URL Service

↓

Generate Short Code

↓

Store Mapping

↓

Return Short URL
```

Example:

```
Original URL

https://example.com/article/12345


Generated

abc123
```

---

## Redirect Flow

Redirect is the most latency-sensitive operation.

Flow:

```
Client

↓

Load Balancer

↓

URL Service

↓

Redis Lookup

↓

Redirect Response
```

Priority:

1. Redis
2. MongoDB fallback

---

## Redirect Strategy

Cache Aside Pattern

Flow:

```
Request

↓

Check Redis

↓

Cache Hit

    return URL


Cache Miss

    Query MongoDB

    Update Redis

    return URL
```

---

# Redis

## Responsibility

Redis is used for:

* URL mapping cache.
* Hot URL acceleration.
* Click counter aggregation.

---

# URL Cache

Key:

```
short_code
```

Value:

```
original_url
```

Example:

```
abc123

=

https://example.com/page
```

---

## Cache Policy

Requirements:

* Define TTL.
* Prevent unlimited memory growth.
* Monitor hit ratio.

---

# Kafka Producer

## Responsibility

Publish analytics events asynchronously.

The redirect request must not wait for analytics processing.

---

## Event Example

```json
{
 "shortCode":"abc123",
 "timestamp":"2026-01-01T10:00:00",
 "ip":"xxx",
 "userAgent":"browser"
}
```

---

# Kafka Cluster

## Responsibility

Provide:

* High throughput.
* Message durability.
* Event buffering.
* Consumer decoupling.

Kafka allows analytics processing to scale independently from URL redirect traffic.

---

# Analytics Consumer

## Responsibility

Consume click events from Kafka.

Processing:

```
Kafka Message

↓

Validate Event

↓

Aggregate Counter

↓

Update Redis Counter
```

---

# Redis Counter

## Purpose

Avoid writing every click directly into MongoDB.

Without Redis:

```
100000 clicks

=

100000 database writes
```

With Redis:

```
100000 clicks

↓

Redis aggregation

↓

Batch database update
```

---

# Counter Strategy

Example:

Key:

```
counter:{shortCode}
```

Value:

```
5000
```

Meaning:

```
5000 clicks since last flush
```

---

# Periodic Flush

A scheduled process periodically writes aggregated data.

Example:

```
Every 5 seconds

Redis Counter

↓

Batch Update

↓

MongoDB
```

---

# MongoDB

## Responsibility

Persistent storage.

Stores:

* URL mapping.
* User information.
* Analytics summary.
* Historical data.

---

# Database Design

## URL Table

Example:

```
url_mapping

id

short_code

original_url

created_at

expired_at

user_id
```

---

## Analytics Table

Example:

```
url_statistics

id

short_code

click_count

updated_at
```

---

# Data Flow

## Short URL Creation

```
Client

↓

URL Service

↓

Generate Code

↓

MongoDB Insert

↓

Redis Cache

↓

Return URL
```

---

## Redirect

```
Client

↓

URL Service

↓

Redis Lookup

↓

Return Redirect

↓

Publish Kafka Event
```

Important:

Redirect response must not wait for Kafka consumer.

---

## Analytics

```
Redirect Event

↓

Kafka

↓

Consumer

↓

Redis Counter

↓

Scheduled Flush

↓

MongoDB
```

---

# Scalability Strategy

## URL Service

Stateless.

Can scale horizontally.

Example:

```
URL Service Instance 1

URL Service Instance 2

URL Service Instance 3
```

---

## Redis

Scale using:

* Redis Cluster.
* Replication.
* Appropriate eviction policy.

---

## Kafka

Scale using:

* Multiple partitions.
* Multiple consumers.
* Consumer groups.

---

## MongoDB

Scale using:

* Index optimization.
* Read replica.
* Partitioning.

---

# Performance Considerations

## Redirect Latency

Critical path:

```
Client

↓

Load Balancer

↓

URL Service

↓

Redis

↓

Response
```

Avoid:

* Database query.
* Analytics processing.
* Heavy computation.

---

# Failure Handling

---

## Redis Failure

Behavior:

```
Redis unavailable

↓

Query MongoDB

↓

Rebuild Cache

```

Impact:

* Higher latency.
* No data loss.

---

## Kafka Failure

Behavior:

```
Redirect

↓

Continue response

↓

Retry event publishing

```

Analytics may be delayed.

Redirect availability has higher priority.

---

## MongoDB Failure

Behavior:

Depends on operation.

Create URL:

* Return error.

Redirect:

* Depends on Redis availability.

---

# Design Decisions

## Why Redis before MongoDB?

Reason:

* Redirect traffic is read-heavy.
* Reduce database load.
* Improve latency.

---

## Why Kafka?

Reason:

* Analytics should not affect redirect latency.
* Decouple write-heavy analytics workload.
* Allow independent scaling.

---

## Why Redis Counter before MongoDB?

Reason:

* Avoid one database write per click.
* Support high-frequency events.
* Batch persistence improves throughput.

---

# Non-Functional Requirements

## Availability

Target:

High availability for redirect service.

---

## Latency

Redirect:

Target < 50ms when cache hit.

---

## Consistency

URL mapping:

Strong consistency.

Analytics:

Eventually consistent.

---

## Observability

Required:

* Metrics.
* Logs.
* Distributed tracing.
* Kafka lag monitoring.
* Redis hit ratio.
* Database latency.

---

# AI Agent Rules

Before modifying this system, always understand:

1. Request flow.
2. Critical path.
3. Data ownership.
4. Cache strategy.
5. Async processing.
6. Failure scenarios.
7. Scalability impact.

Never introduce changes that increase redirect latency without justification.
