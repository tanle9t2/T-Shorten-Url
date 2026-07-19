package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.request.ShorUrlCreatedRequest;
import com.tanle.t_shorten_url.response.ShortUrlResponse;

import java.util.Optional;

public interface ShortUrlService {
    ShortUrlResponse findByShortUrl(String shortUrl);

    String findRedirectUrl(String code) throws InterruptedException;

    String save(ShorUrlCreatedRequest shortUrl);
}
