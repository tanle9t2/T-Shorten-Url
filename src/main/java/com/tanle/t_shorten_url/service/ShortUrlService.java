package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.request.ShortUrlCreatedRequest;
import com.tanle.t_shorten_url.request.ShortUrlRequest;
import com.tanle.t_shorten_url.request.ShortUrlUpdateRequest;
import com.tanle.t_shorten_url.response.ShortUrlResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ShortUrlService {
    ShortUrlResponse findByShortUrl(String shortUrl);

    String findRedirectUrl(String code, HttpServletRequest request) throws InterruptedException;

    String save(ShortUrlCreatedRequest shortUrl);

    void updateShortUrl(ShortUrlUpdateRequest shortUrl);
}
