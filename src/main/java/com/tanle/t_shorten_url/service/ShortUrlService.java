package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.request.ShortUrlCreatedRequest;
import com.tanle.t_shorten_url.request.ShortUrlUpdateRequest;
import com.tanle.t_shorten_url.response.PageResponse;
import com.tanle.t_shorten_url.response.ShortUrlDetailResponse;
import com.tanle.t_shorten_url.response.ShortUrlResponse;
import com.tanle.t_shorten_url.response.TotalViewUrlResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ShortUrlService {
    ShortUrlResponse findByShortUrl(String shortUrl);

    ShortUrlDetailResponse findById(String id);

    String findRedirectUrl(String code, HttpServletRequest request) throws InterruptedException;

    ShortUrlResponse save(ShortUrlCreatedRequest shortUrl);

    void deleteShortUrlById(String id);

    TotalViewUrlResponse getTotalView(String code);

    void updateShortUrl(ShortUrlUpdateRequest shortUrl);

    PageResponse<ShortUrlResponse> findByUserId(String userId, int page, int size, String sortBy, String order);
}
