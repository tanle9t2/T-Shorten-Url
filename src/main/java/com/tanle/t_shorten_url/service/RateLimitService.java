package com.tanle.t_shorten_url.service;

public interface RateLimitService {
    boolean tryConsume(String clientId);
}
